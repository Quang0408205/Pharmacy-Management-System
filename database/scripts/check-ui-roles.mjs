import puppeteer from "puppeteer-core";

const executablePath =
  process.env.BROWSER_PATH ||
  "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const baseUrl = process.env.BASE_URL || "http://localhost:8088";
const adminPassword = process.env.ADMIN_PASSWORD || "Pharmacy@123";
const staffPassword = "MatKhau@12345";
const suffix = String(Date.now()).slice(-6);
const api = `${baseUrl}/api/v1`;

const salesPages = ["pos", "invoices", "customers", "lookup", "history"];
const warehousePages = [
  "dashboard",
  "receiving",
  "receipts",
  "inventory",
  "batches",
  "expiry",
  "stocktake",
  "disposals",
  "suppliers",
  "movements",
];

const pause = (milliseconds) =>
  new Promise((resolveWait) => setTimeout(resolveWait, milliseconds));
const problems = [];
const record = (message) => {
  problems.push(message);
  process.stdout.write(`  PROBLEM ${message}\n`);
};
const note = (message) => process.stdout.write(`${message}\n`);

const apiSession = () => {
  let cookie = "";
  return async (method, path, body) => {
    const response = await fetch(`${api}${path}`, {
      method,
      headers: {
        Accept: "application/json",
        ...(body === undefined ? {} : { "Content-Type": "application/json" }),
        ...(cookie ? { Cookie: cookie } : {}),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const token = (response.headers.getSetCookie?.() ?? []).find((item) =>
      item.startsWith("PMS_TOKEN="),
    );
    if (token) cookie = token.split(";")[0];
    const text = await response.text();
    return { status: response.status, data: text ? JSON.parse(text) : null };
  };
};

const date = (offsetDays) =>
  new Date(Date.now() + offsetDays * 86400000).toISOString().slice(0, 10);

const prepare = async () => {
  const admin = apiSession();
  await admin("POST", "/auth/login", {
    identifier: "admin",
    password: adminPassword,
  });
  const created = {};
  for (const [role, label] of [
    ["SALES", "ui.ban"],
    ["WAREHOUSE", "ui.kho"],
  ]) {
    const username = `${label}${suffix}`;
    const result = await admin("POST", "/employees", {
      name: `Kiểm thử ${label} ${suffix}`,
      email: `${username}@nhathuoc.vn`,
      username,
      phone: "0900000000",
      role,
    });
    const token = new URL(
      result.data.activationLink,
      baseUrl,
    ).searchParams.get("token");
    await admin("POST", "/auth/activate", { token, password: staffPassword });
    created[role] = username;
  }
  const warehouse = apiSession();
  await warehouse("POST", "/auth/login", {
    identifier: created.WAREHOUSE,
    password: staffPassword,
  });
  const medicines = await admin(
    "GET",
    "/medicines?size=50&status=ACTIVE&prescriptionType=OTC&sort=name,asc",
  );
  const medicine = medicines.data.items.find((item) => item.price > 0);
  const suppliers = await warehouse("GET", "/suppliers?status=ACTIVE&size=1");
  await warehouse("POST", "/receipts", {
    supplierId: suppliers.data.items[0].id,
    receivingDate: date(0),
    confirm: true,
    items: [
      {
        medicineId: medicine.id,
        batchCode: `UI${suffix}`,
        manufactureDate: date(-20),
        expiryDate: date(200),
        quantity: 40,
        purchasePrice: 1000,
      },
    ],
  });
  const batches = await warehouse(
    "GET",
    `/batches?medicineId=${medicine.id}&q=UI${suffix}`,
  );
  return { created, medicine, batchId: batches.data.items[0].id, admin };
};

const watch = (page, label) => {
  page.on("response", (response) => {
    const url = response.url();
    if (
      url.includes("/api/v1/") &&
      response.status() >= 400 &&
      !(response.status() === 401 && url.includes("/auth/me"))
    )
      record(
        `${label}: ${response.request().method()} ${url.split("/api/v1")[1]} -> ${response.status()}`,
      );
  });
  page.on("pageerror", (error) =>
    record(`${label}: pageerror ${error.message}`),
  );
  page.on("console", (message) => {
    if (
      message.type() === "error" &&
      !message.text().includes("Failed to load resource")
    )
      record(`${label}: console ${message.text()}`);
  });
};

const login = async (page, identifier) => {
  await page.goto(`${baseUrl}/pages/auth/login.html`, {
    waitUntil: "networkidle2",
  });
  await page.type("#identifier", identifier);
  await page.type("#password", staffPassword);
  await Promise.all([
    page
      .waitForNavigation({ waitUntil: "networkidle2", timeout: 30000 })
      .catch(() => null),
    page.click('button[type="submit"]'),
  ]);
  await pause(1500);
};

const visit = async (page, role, names) => {
  for (const name of names) {
    await page.goto(`${baseUrl}/pages/${role}/${name}.html`, {
      waitUntil: "networkidle2",
    });
    await pause(1200);
    const alerts = await page.$$eval(".alert--danger:not(.hidden)", (items) =>
      items.map((item) => item.innerText).filter(Boolean),
    );
    const rows = await page
      .$$eval("tbody tr", (items) => items.length)
      .catch(() => 0);
    note(
      `${role}/${name}: rows=${rows}${alerts.length ? ` alerts=${alerts.join("|")}` : ""}`,
    );
    if (alerts.length) record(`${role}/${name}: ${alerts.join("|")}`);
  }
};

const setup = await prepare();
const browser = await puppeteer.launch({
  executablePath,
  headless: true,
  args: ["--no-sandbox"],
});
try {
  const salesContext = await browser.createBrowserContext();
  const sales = await salesContext.newPage();
  watch(sales, "sales");
  await login(sales, setup.created.SALES);
  note(`sales landed on ${new URL(sales.url()).pathname}`);
  if (!sales.url().includes("/sales/")) record("sales: sai trang chủ");
  await visit(sales, "sales", salesPages);

  await sales.goto(`${baseUrl}/pages/sales/pos.html`, {
    waitUntil: "networkidle2",
  });
  await pause(800);
  await sales.type("[data-search]", setup.medicine.name.slice(0, 6));
  await pause(1200);
  const card = await sales.$(`[data-add="${setup.medicine.id}"]`);
  if (!card) record("pos: không thấy thuốc vừa nhập trong danh sách bán");
  else {
    await card.click();
    await pause(500);
    await sales.type("[data-received]", "100000000");
    await sales.click("[data-checkout]");
    await pause(2000);
    const success = await sales
      .$eval(".modal", (element) => element.innerText)
      .catch(() => "");
    note(`pos: hộp thoại sau thanh toán: ${success.slice(0, 80).replace(/\n/g, " ")}`);
    if (!/HD-/.test(success)) record("pos: thanh toán không hiện mã hóa đơn");
  }
  await visit(sales, "sales", ["history", "invoices"]);

  const warehouseContext = await browser.createBrowserContext();
  const warehouse = await warehouseContext.newPage();
  watch(warehouse, "warehouse");
  await login(warehouse, setup.created.WAREHOUSE);
  note(`warehouse landed on ${new URL(warehouse.url()).pathname}`);
  if (!warehouse.url().includes("/warehouse/"))
    record("warehouse: sai trang chủ");
  await visit(warehouse, "warehouse", warehousePages);

  await warehouse.goto(
    `${baseUrl}/pages/warehouse/disposals.html?batchId=${setup.batchId}`,
    { waitUntil: "networkidle2" },
  );
  await pause(1500);
  const hasDialog = await warehouse.$(".modal [data-disposal-form]");
  if (!hasDialog) record("disposals: không mở biểu mẫu lập phiếu từ liên kết lô");
  else {
    await warehouse.type("[data-reason]", "Kiểm thử giao diện");
    await warehouse.click("[data-save]");
    await pause(2000);
    const drawer = await warehouse
      .$eval(".drawer", (element) => element.innerText)
      .catch(() => "");
    note(`disposals: ngăn chi tiết: ${drawer.slice(0, 60).replace(/\n/g, " ")}`);
    if (!/XH-/.test(drawer)) record("disposals: không hiện phiếu vừa lập");
    const submit = await warehouse.$('.drawer [data-act="submit"]');
    if (!submit) record("disposals: thiếu nút Gửi duyệt");
    else {
      await pause(800);
      await submit.evaluate((element) => element.click());
      await warehouse
        .waitForSelector('.modal [data-dismiss="confirm"]', { timeout: 5000 })
        .catch(async () => {
          record(
            `disposals: không hiện hộp xác nhận: ${await warehouse.$$eval('[role=dialog]', (items) => items.map((item) => item.className).join(','))}`,
          );
        });
      await warehouse.click('.modal [data-dismiss="confirm"]').catch(() => null);
      await pause(1500);
      const after = await warehouse
        .$eval(".drawer", (element) => element.innerText)
        .catch(() => "");
      if (!/Chờ duyệt/.test(after)) record("disposals: phiếu không chuyển Chờ duyệt");
      else note("disposals: gửi duyệt thành công");
    }
  }
} finally {
  await browser.close();
}

note(problems.length ? `\n${problems.length} VẤN ĐỀ` : "\nGIAO DIỆN CÁC VAI TRÒ ĐẠT");
process.exit(problems.length ? 1 : 0);
