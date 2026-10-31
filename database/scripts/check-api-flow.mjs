const baseUrl = process.env.BASE_URL || "http://localhost:8088";
const adminPassword = process.env.ADMIN_PASSWORD || "Pharmacy@123";
const staffPassword = "MatKhau@12345";
const suffix = String(Date.now()).slice(-6);
const api = `${baseUrl}/api/v1`;

let failures = 0;
const check = (label, condition, detail = "") => {
  process.stdout.write(`${condition ? "  OK  " : "  FAIL"} ${label}${condition ? "" : ` ${detail}`}\n`);
  if (!condition) failures += 1;
};

const session = (name) => {
  let cookie = "";
  const call = async (method, path, body) => {
    const response = await fetch(`${api}${path}`, {
      method,
      headers: {
        Accept: "application/json",
        ...(body === undefined ? {} : { "Content-Type": "application/json" }),
        ...(cookie ? { Cookie: cookie } : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const setCookie = response.headers.getSetCookie?.() ?? [];
    const token = setCookie.find((item) => item.startsWith("PMS_TOKEN="));
    if (token) cookie = token.split(";")[0];
    const text = await response.text();
    return { status: response.status, data: text ? JSON.parse(text) : null };
  };
  return { name, call };
};

const login = async (client, identifier, password) => {
  const result = await client.call("POST", "/auth/login", { identifier, password, remember: false });
  check(`${client.name}: đăng nhập`, result.status === 200, JSON.stringify(result.data));
};

const createStaff = async (admin, role, label) => {
  const username = `${label}${suffix}`;
  const created = await admin.call("POST", "/employees", {
    name: `Nhân viên ${label} ${suffix}`,
    email: `${username}@nhathuoc.vn`,
    username,
    phone: "0900000000",
    role,
  });
  check(`admin: tạo nhân viên ${role}`, created.status === 200 || created.status === 201, JSON.stringify(created.data));
  const token = new URL(created.data.activationLink, baseUrl).searchParams.get("token");
  const activated = await admin.call("POST", "/auth/activate", { token, password: staffPassword });
  check(`${username}: kích hoạt bằng link`, activated.status === 200, JSON.stringify(activated.data));
  return username;
};

const today = new Date();
const dateKey = (offsetDays) => {
  const value = new Date(today.getTime() + offsetDays * 86400000);
  return value.toISOString().slice(0, 10);
};

const admin = session("admin");
await login(admin, "admin", adminPassword);

const salesUser = await createStaff(admin, "SALES", "ban");
const warehouseUser = await createStaff(admin, "WAREHOUSE", "kho");
const sales = session("sales");
const warehouse = session("warehouse");
await login(sales, salesUser, staffPassword);
await login(warehouse, warehouseUser, staffPassword);

const medicines = await admin.call("GET", "/medicines?size=50&status=ACTIVE&prescriptionType=OTC&sort=name,asc");
const medicine = medicines.data.items.find((item) => item.price > 0);
check("có thuốc OTC đang kinh doanh để thử", Boolean(medicine));
const suppliers = await warehouse.call("GET", "/suppliers?status=ACTIVE&size=5");
const supplier = suppliers.data.items[0];
check("kho xem được nhà cung cấp", Boolean(supplier));

const batchCode = `E2E${suffix}`;
const receipt = await warehouse.call("POST", "/receipts", {
  supplierId: supplier.id,
  receivingDate: dateKey(0),
  confirm: true,
  items: [
    {
      medicineId: medicine.id,
      batchCode,
      manufactureDate: dateKey(-30),
      expiryDate: dateKey(300),
      quantity: 60,
      purchasePrice: 1000,
    },
  ],
});
check("kho: xác nhận phiếu nhập", receipt.status === 200 && receipt.data.status === "CONFIRMED", JSON.stringify(receipt.data));

const lookup = await sales.call("GET", `/medicines?size=5&sellable=true&q=${encodeURIComponent(medicine.name)}`);
const sellable = lookup.data.items.find((item) => item.id === medicine.id);
check("bán thuốc: thấy tồn khả dụng sau khi nhập", sellable && sellable.availableQty >= 60, JSON.stringify(sellable));
const batchesForSales = await sales.call("GET", `/batches?medicineId=${medicine.id}`);
check("bán thuốc: không thấy giá nhập của lô", batchesForSales.data.items.every((item) => item.purchasePrice === undefined));

const preview = await sales.call("GET", "/invoices/next-number");
check("bán thuốc: xem trước mã hóa đơn", preview.status === 200 && /^HD-/.test(preview.data.code));

const draft = await sales.call("POST", "/invoice-drafts", {
  items: [{ medicineId: medicine.id, quantity: 2 }],
  discountType: "AMOUNT",
  discountValue: 0,
  paymentMethod: "CASH",
});
check("bán thuốc: lưu đơn nháp", draft.status === 200, JSON.stringify(draft.data));

const requestId = `e2e-${suffix}`;
const checkoutBody = {
  items: [{ medicineId: medicine.id, quantity: 10 }],
  discountType: "PERCENT",
  discountValue: 10,
  paymentMethod: "CASH",
  receivedAmount: 100000000,
  draftId: draft.data.id,
  requestId,
};
const invoice = await sales.call("POST", "/invoices", checkoutBody);
check("bán thuốc: thanh toán", invoice.status === 200 && invoice.data.status === "COMPLETED", JSON.stringify(invoice.data));
const expectedSubtotal = medicine.price * 10;
check("server tự tính tiền", invoice.data.subtotal === expectedSubtotal && invoice.data.total === expectedSubtotal - Math.round(expectedSubtotal / 10));
const again = await sales.call("POST", "/invoices", checkoutBody);
check("gửi lại requestId trả hóa đơn cũ", again.status === 200 && again.data.id === invoice.data.id);
const drafts = await sales.call("GET", "/invoice-drafts");
check("đơn nháp bị xóa sau thanh toán", drafts.data.length === 0);
const dashboard = await sales.call("GET", "/dashboard/sales");
check("dashboard bán hàng đếm đúng", dashboard.data.invoicesToday === 1 && dashboard.data.revenueToday === invoice.data.total, JSON.stringify(dashboard.data));
const denied = await warehouse.call("POST", "/invoices", checkoutBody);
check("kho không được bán hàng (403)", denied.status === 403);

const cancelled = await admin.call("POST", `/invoices/${invoice.data.id}/cancel`, { reason: "Kiểm thử luồng" });
check("admin: hủy hóa đơn hoàn kho", cancelled.status === 200 && cancelled.data.status === "CANCELLED", JSON.stringify(cancelled.data));

const batches = await warehouse.call("GET", `/batches?medicineId=${medicine.id}&q=${batchCode}`);
const batch = batches.data.items.find((item) => item.code === batchCode);
check("lô trở lại đủ 60 sau khi hủy hóa đơn", batch && batch.remainingQty === 60, JSON.stringify(batch));

const stocktake = await warehouse.call("POST", "/stocktakes", { categoryId: "ALL", note: "Kiểm thử" });
check("kho: tạo phiên kiểm kê", stocktake.status === 200 && stocktake.data.lines.length > 0, JSON.stringify(stocktake.data));
const lines = stocktake.data.lines.map((line) => ({
  batchId: line.batchId,
  actualQty: line.batchId === batch.id ? 58 : line.systemQty,
  reason: line.batchId === batch.id ? "Vỡ vỉ" : "",
}));
const submitted = await warehouse.call("POST", `/stocktakes/${stocktake.data.id}/submit`, { lines });
check("kho: gửi duyệt kiểm kê", submitted.status === 200 && submitted.data.status === "PENDING_APPROVAL", JSON.stringify(submitted.data));
const approved = await admin.call("POST", `/stocktakes/${stocktake.data.id}/approve`, { note: "Đã duyệt" });
check("admin: duyệt kiểm kê", approved.status === 200 && approved.data.status === "APPROVED", JSON.stringify(approved.data));

const disposal = await warehouse.call("POST", "/disposals", {
  reason: "Kiểm thử xuất hủy",
  lines: [{ batchId: batch.id, quantity: 8, reasonType: "DAMAGED" }],
});
check("kho: lập phiếu xuất hủy", disposal.status === 200, JSON.stringify(disposal.data));
const disposalSubmit = await warehouse.call("POST", `/disposals/${disposal.data.id}/submit`);
check("kho: gửi duyệt xuất hủy", disposalSubmit.status === 200, JSON.stringify(disposalSubmit.data));
const premature = await warehouse.call("POST", `/disposals/${disposal.data.id}/execute`);
check("không thực hiện được khi chưa duyệt (409)", premature.status === 409);
const disposalApprove = await admin.call("POST", `/disposals/${disposal.data.id}/approve`, { note: "Đồng ý" });
check("admin: duyệt xuất hủy", disposalApprove.status === 200, JSON.stringify(disposalApprove.data));
const executed = await warehouse.call("POST", `/disposals/${disposal.data.id}/execute`);
check("kho: thực hiện xuất hủy", executed.status === 200 && executed.data.status === "EXECUTED", JSON.stringify(executed.data));
const finalBatch = (await warehouse.call("GET", `/batches/${batch.id}`)).data;
check("lô còn 50 (60 - 2 kiểm kê - 8 hủy)", finalBatch.remainingQty === 50, JSON.stringify(finalBatch.remainingQty));

const warehouseDashboard = await warehouse.call("GET", "/dashboard/warehouse");
check("dashboard kho trả đủ phần", warehouseDashboard.status === 200 && warehouseDashboard.data.movementSeries.length === 14 && warehouseDashboard.data.tasks.length === 6, JSON.stringify(warehouseDashboard.data).slice(0, 200));
const expiry = await warehouse.call("GET", "/expiry/summary");
check("tóm tắt hạn dùng", expiry.status === 200 && Array.isArray(expiry.data.tiers));

const reconciliation = await admin.call("GET", "/inventory/reconciliation");
check("đối soát sổ kho không có lô lệch", reconciliation.status === 200 && reconciliation.data.balanced === true, JSON.stringify(reconciliation.data));

process.stdout.write(`\n${failures === 0 ? "TẤT CẢ BƯỚC ĐẠT" : `${failures} BƯỚC LỖI`}\n`);
process.exit(failures === 0 ? 0 : 1);
