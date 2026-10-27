import {
  categoryCatalog,
  customerNames,
  employeeSeed,
  ingredientCatalog,
  manufacturers,
  supplierCatalog,
} from './catalog.js';
import { addDays, addDaysKey, toDateKey } from '../core/format.js';

const createRandom = (seed) => {
  let state = seed >>> 0;
  const next = () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
  const int = (min, max) => Math.floor(next() * (max - min + 1)) + min;
  const pick = (list) => list[Math.floor(next() * list.length)];
  const shuffle = (list) => {
    const copy = [...list];
    for (let index = copy.length - 1; index > 0; index -= 1) {
      const swap = Math.floor(next() * (index + 1));
      [copy[index], copy[swap]] = [copy[swap], copy[index]];
    }
    return copy;
  };
  const chance = (probability) => next() < probability;
  return { next, int, pick, shuffle, chance };
};

const pad = (value, size) => String(value).padStart(size, '0');
const roundTo = (value, step) => Math.round(value / step) * step;

const packagingFor = (form, unit, random) => {
  if (unit === 'Cái') return 'Hộp 1 cái';
  if (unit === 'Chai') return `Chai ${random.pick([60, 100, 120, 250])} ml`;
  if (unit === 'Lọ') return `Lọ ${random.pick([10, 15, 30, 60])} ${/nhỏ|xịt|tiêm/i.test(form) ? 'ml' : 'viên'}`;
  if (unit === 'Tuýp') return `Tuýp ${random.pick([10, 15, 20, 30])} ${/viên sủi/i.test(form) ? 'viên' : 'g'}`;
  if (unit === 'Vỉ') return `Vỉ ${random.pick([7, 10, 21, 28])} viên`;
  if (unit === 'Gói') return `Gói ${random.pick([50, 100, 200])} g`;
  if (/cốm|bột|gói/i.test(form)) return `Hộp ${random.pick([10, 20, 30])} gói`;
  if (/que|khẩu trang|băng/i.test(form)) return `Hộp ${random.pick([25, 50, 100])} cái`;
  return `Hộp ${random.pick([1, 2, 3, 5, 10])} vỉ × ${random.pick([10, 10, 12, 14])} viên`;
};

const buildCategories = () =>
  categoryCatalog.map((category, index) => ({
    id: `cat-${pad(index + 1, 2)}`,
    code: category.code,
    name: category.name,
    color: category.color,
    description: `Nhóm ${category.name.toLowerCase()} tại nhà thuốc`,
    status: 'ACTIVE',
  }));

const buildSuppliers = (random, now) =>
  supplierCatalog.map(([name, contactName, phone, city, taxCode], index) => ({
    id: `sup-${pad(index + 1, 2)}`,
    code: `NCC${pad(index + 1, 3)}`,
    name,
    contactName,
    phone,
    email: `lienhe${index + 1}@${name
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .replace(/[^a-zA-Z]/g, '')
      .toLowerCase()
      .slice(0, 12)}.vn`,
    address: `${random.int(12, 480)} Đường ${random.pick(['Lê Lợi', 'Nguyễn Trãi', 'Trần Hưng Đạo', 'Hai Bà Trưng', 'Lý Thường Kiệt'])}, ${city}`,
    taxCode,
    paymentTerms: random.pick(['Thanh toán ngay', 'Công nợ 15 ngày', 'Công nợ 30 ngày']),
    status: index === 13 ? 'INACTIVE' : 'ACTIVE',
    note: '',
    createdAt: addDays(now, -random.int(400, 900)).toISOString(),
  }));

const buildMedicines = (random, categories, now) => {
  const categoryByCode = Object.fromEntries(categories.map((category) => [category.code, category]));
  const variants = [];
  ingredientCatalog.forEach(([categoryCode, ingredient, strengths, form, unit, price, rx]) => {
    strengths.forEach((strength, strengthIndex) => {
      variants.push({ categoryCode, ingredient, strength, form, unit, price, rx, strengthIndex });
    });
  });

  const medicines = [];
  let pass = 0;
  while (medicines.length < 500) {
    for (let index = 0; index < variants.length && medicines.length < 500; index += 1) {
      const variant = variants[index];
      const manufacturer = manufacturers[(index * 3 + pass * 5) % manufacturers.length];
      const strengthFactor = 1 + variant.strengthIndex * 0.38;
      const price = Math.max(2000, roundTo(variant.price * strengthFactor * manufacturer.factor, 500));
      const serial = medicines.length + 1;
      medicines.push({
        id: `med-${pad(serial, 4)}`,
        code: `T${pad(serial, 4)}`,
        barcode: `893${pad(Math.floor(random.next() * 1e10), 10)}`,
        name: `${variant.ingredient} ${variant.strength}`,
        genericName: variant.ingredient,
        strength: variant.strength,
        dosageForm: variant.form,
        unit: variant.unit,
        packaging: packagingFor(variant.form, variant.unit, random),
        manufacturer: manufacturer.name,
        country: manufacturer.country,
        categoryId: categoryByCode[variant.categoryCode].id,
        price,
        costPrice: roundTo(price * (0.62 + random.next() * 0.14), 100),
        prescriptionType: variant.rx,
        status: random.chance(0.045) ? 'INACTIVE' : 'ACTIVE',
        minStock: random.pick([10, 15, 20, 20, 30, 40, 50]),
        description: `${variant.ingredient} ${variant.strength} dạng ${variant.form.toLowerCase()} do ${manufacturer.name} sản xuất.`,
        imageUrl: null,
        createdAt: addDays(now, -random.int(200, 800)).toISOString(),
      });
    }
    pass += 1;
  }
  return medicines;
};

const buildEmployees = (now) =>
  employeeSeed.map((employee, index) => ({
    id: `emp-${pad(index + 1, 2)}`,
    ...employee,
    status: index === 5 ? 'INACTIVE' : 'ACTIVE',
    createdAt: addDays(now, -(300 - index * 20)).toISOString(),
    lastLoginAt: null,
  }));

const buildCustomers = (random, now) => {
  const families = ['Nguyễn', 'Trần', 'Lê', 'Phạm', 'Hoàng', 'Vũ', 'Đặng', 'Bùi', 'Đỗ', 'Ngô'];
  const middles = ['Văn', 'Thị', 'Minh', 'Quốc', 'Thu', 'Hồng', 'Ngọc', 'Gia', 'Anh', 'Thanh'];
  const givens = customerNames.map((name) => name.split(' ').pop());
  const seen = new Set();
  const customers = [];
  while (customers.length < 100) {
    const name = `${random.pick(families)} ${random.pick(middles)} ${random.pick(givens)}`;
    if (seen.has(name)) continue;
    seen.add(name);
    const serial = customers.length + 1;
    const phone = `09${random.int(0, 9)}${random.int(0, 9)} ${pad(random.int(0, 999), 3)} ${pad(random.int(0, 999), 3)}`;
    customers.push({
      id: `cus-${pad(serial, 3)}`,
      code: `KH${pad(serial, 4)}`,
      name,
      phone,
      email: random.chance(0.4) ? `khach${serial}@example.vn` : '',
      address: `${random.int(1, 300)} Đường ${random.pick(['Lê Lợi', 'Nguyễn Huệ', 'Pasteur', 'Cách Mạng Tháng 8', 'Phan Chu Trinh'])}`,
      birthYear: random.int(1950, 2008),
      gender: random.pick(['Nam', 'Nữ']),
      note: random.chance(0.15) ? 'Khách thân thiết' : '',
      createdAt: addDays(now, -random.int(5, 400)).toISOString(),
    });
  }
  return customers;
};

const batchCode = (receivedKey, serial) => `L${receivedKey.slice(2, 4)}${receivedKey.slice(5, 7)}-${pad(serial, 4)}`;

const buildReceiptsAndBatches = (random, context) => {
  const { medicines, suppliers, employees, now } = context;
  const warehouseStaff = employees.filter((employee) => employee.role === 'WAREHOUSE');
  const confirmedCount = 96;
  const todayKey = toDateKey(now);

  const dayOffsets = Array.from(
    { length: confirmedCount },
    () => 1 + Math.floor(Math.pow(random.next(), 1.5) * 540),
  ).sort((a, b) => b - a);
  const slots = random
    .shuffle(medicines.map((medicine) => medicine.id))
    .concat(Array.from({ length: 500 }, () => random.pick(medicines).id));
  const slotOrder = random.shuffle(slots);
  const medicineById = Object.fromEntries(medicines.map((medicine) => [medicine.id, medicine]));

  const receipts = [];
  const batches = [];
  const movements = [];
  let slotIndex = 0;

  dayOffsets.forEach((offset, receiptIndex) => {
    const receivedDate = addDaysKey(now, -offset);
    const supplier = random.pick(suppliers.filter((item) => item.status === 'ACTIVE'));
    const employee = random.pick(warehouseStaff);
    const itemCount = receiptIndex < 40 ? 11 : 10;
    const items = [];

    for (let line = 0; line < itemCount; line += 1) {
      const medicine = medicineById[slotOrder[slotIndex]];
      slotIndex += 1;
      const batchSerial = batches.length + 1;
      let expiry;
      const roll = random.next();
      if (roll < 0.04 && offset > 150) {
        expiry = addDaysKey(now, -random.int(1, 100));
      } else if (roll < 0.18) {
        expiry = addDaysKey(now, random.int(5, 95));
      } else {
        expiry = addDaysKey(now, random.int(100, 900));
      }
      if (expiry <= addDaysKey(receivedDate, 20)) expiry = addDaysKey(now, random.int(150, 700));
      const manufactureDate = addDaysKey(receivedDate, -random.int(30, 280));
      const expensive = medicine.price >= 100000;
      const initialQty = expensive ? random.int(8, 40) * 5 : random.int(6, 40) * 10;
      const purchasePrice = roundTo(medicine.costPrice * (0.95 + random.next() * 0.1), 100);
      const batch = {
        id: `bat-${pad(batchSerial, 4)}`,
        code: batchCode(receivedDate, batchSerial),
        medicineId: medicine.id,
        supplierId: supplier.id,
        receiptId: `rec-${pad(receiptIndex + 1, 3)}`,
        receivedDate,
        manufactureDate,
        expiryDate: expiry,
        initialQty,
        remainingQty: initialQty,
        purchasePrice,
      };
      batches.push(batch);
      items.push({
        medicineId: medicine.id,
        medicineCode: medicine.code,
        medicineName: medicine.name,
        batchId: batch.id,
        batchCode: batch.code,
        manufactureDate,
        expiryDate: expiry,
        quantity: initialQty,
        unit: medicine.unit,
        purchasePrice,
        subtotal: initialQty * purchasePrice,
      });
    }

    const receipt = {
      id: `rec-${pad(receiptIndex + 1, 3)}`,
      code: `PN-${receivedDate.slice(0, 4)}-${pad(receiptIndex + 1, 4)}`,
      supplierId: supplier.id,
      supplierName: supplier.name,
      status: 'CONFIRMED',
      receivingDate: receivedDate,
      createdById: employee.id,
      createdByName: employee.name,
      confirmedAt: new Date(`${receivedDate}T09:30:00`).toISOString(),
      items,
      totalQuantity: items.reduce((sum, item) => sum + item.quantity, 0),
      totalAmount: items.reduce((sum, item) => sum + item.subtotal, 0),
      note: '',
    };
    receipts.push(receipt);
    items.forEach((item) => {
      movements.push({
        createdAt: receipt.confirmedAt,
        type: 'IN',
        medicineId: item.medicineId,
        medicineName: item.medicineName,
        batchId: item.batchId,
        batchCode: item.batchCode,
        quantity: item.quantity,
        referenceType: 'RECEIPT',
        referenceCode: receipt.code,
        performedById: employee.id,
        performedByName: employee.name,
        note: `Nhập từ ${supplier.name}`,
      });
    });
  });

  for (let draftIndex = 0; draftIndex < 4; draftIndex += 1) {
    const supplier = random.pick(suppliers.filter((item) => item.status === 'ACTIVE'));
    const employee = random.pick(warehouseStaff);
    const items = Array.from({ length: random.int(2, 4) }, () => {
      const medicine = random.pick(medicines);
      const quantity = random.int(5, 30) * 10;
      const purchasePrice = roundTo(medicine.costPrice, 100);
      return {
        medicineId: medicine.id,
        medicineCode: medicine.code,
        medicineName: medicine.name,
        batchCode: `DR${pad(draftIndex + 1, 2)}-${random.int(100, 999)}`,
        manufactureDate: addDaysKey(now, -random.int(30, 120)),
        expiryDate: addDaysKey(now, random.int(300, 900)),
        quantity,
        unit: medicine.unit,
        purchasePrice,
        subtotal: quantity * purchasePrice,
      };
    });
    const receivingDate = addDaysKey(now, -draftIndex);
    receipts.push({
      id: `rec-${pad(confirmedCount + draftIndex + 1, 3)}`,
      code: `PN-${todayKey.slice(0, 4)}-${pad(confirmedCount + draftIndex + 1, 4)}`,
      supplierId: supplier.id,
      supplierName: supplier.name,
      status: 'DRAFT',
      receivingDate,
      createdById: employee.id,
      createdByName: employee.name,
      confirmedAt: null,
      items,
      totalQuantity: items.reduce((sum, item) => sum + item.quantity, 0),
      totalAmount: items.reduce((sum, item) => sum + item.subtotal, 0),
      note: 'Phiếu nháp chờ xác nhận',
    });
  }

  return { receipts, batches, movements };
};

const allocateFefo = (candidates, quantity, atKey) => {
  const usable = candidates
    .filter((batch) => batch.remainingQty > 0 && batch.expiryDate > atKey && batch.receivedDate <= atKey)
    .sort((a, b) => a.expiryDate.localeCompare(b.expiryDate));
  const available = usable.reduce((sum, batch) => sum + batch.remainingQty, 0);
  if (available < quantity) return null;
  const allocations = [];
  let needed = quantity;
  for (const batch of usable) {
    if (needed === 0) break;
    const take = Math.min(batch.remainingQty, needed);
    allocations.push({ batch, quantity: take });
    needed -= take;
  }
  return allocations;
};

const buildInvoices = (random, context) => {
  const { medicines, customers, employees, batches, now } = context;
  const salesStaff = employees.filter((employee) => employee.role === 'SALES');
  const salesWeights = [4, 3, 2, 1];
  const weightedStaff = salesStaff.flatMap((employee, index) => Array(salesWeights[index] || 1).fill(employee));
  const batchesByMedicine = new Map();
  batches.forEach((batch) => {
    if (!batchesByMedicine.has(batch.medicineId)) batchesByMedicine.set(batch.medicineId, []);
    batchesByMedicine.get(batch.medicineId).push(batch);
  });
  const sellable = medicines.filter((medicine) => medicine.status === 'ACTIVE');
  const minutesToday = now.getHours() * 60 + now.getMinutes();

  const timestamps = [];
  for (let index = 0; index < 500; index += 1) {
    if (index < 12 && minutesToday > 20) {
      const minutes = random.int(5, Math.max(6, minutesToday - 1));
      const date = new Date(now);
      date.setHours(0, 0, 0, 0);
      date.setMinutes(minutes);
      timestamps.push(date);
    } else {
      const dayOffset = 1 + Math.floor(Math.pow(random.next(), 1.25) * 89);
      const date = addDays(now, -dayOffset);
      date.setHours(random.int(7, 21), random.int(0, 59), random.int(0, 59), 0);
      timestamps.push(date);
    }
  }
  timestamps.sort((a, b) => a - b);

  const invoices = [];
  const movements = [];
  const dayCounters = {};
  const cancelledIndexes = new Set(random.shuffle(Array.from({ length: 500 }, (_, i) => i)).slice(0, 12));

  timestamps.forEach((date, index) => {
    const dateKey = toDateKey(date);
    const cancelled = cancelledIndexes.has(index);
    const employee = random.pick(weightedStaff);
    const wantedLines = random.pick([1, 1, 2, 2, 2, 3, 3, 4, 5]);
    const items = [];
    const usedMedicines = new Set();
    const applied = [];
    let attempts = 0;

    while (items.length < wantedLines && attempts < 40) {
      attempts += 1;
      const medicine = random.pick(sellable);
      if (usedMedicines.has(medicine.id)) continue;
      const quantity = medicine.price < 40000 ? random.int(1, 4) : random.int(1, 2);
      const allocations = allocateFefo(batchesByMedicine.get(medicine.id) || [], quantity, dateKey);
      if (!allocations) continue;
      usedMedicines.add(medicine.id);
      allocations.forEach(({ batch, quantity: taken }) => {
        batch.remainingQty -= taken;
        applied.push({ batch, quantity: taken, medicine });
      });
      items.push({
        medicineId: medicine.id,
        medicineCode: medicine.code,
        medicineName: medicine.name,
        unit: medicine.unit,
        prescriptionType: medicine.prescriptionType,
        quantity,
        unitPrice: medicine.price,
        subtotal: quantity * medicine.price,
        allocations: allocations.map(({ batch, quantity: taken }) => ({
          batchId: batch.id,
          batchCode: batch.code,
          expiryDate: batch.expiryDate,
          quantity: taken,
        })),
      });
    }
    if (!items.length) return;

    const needsCustomer = items.some((item) => item.prescriptionType !== 'OTC');
    const customer = needsCustomer || random.chance(0.45) ? random.pick(customers) : null;
    const subtotal = items.reduce((sum, item) => sum + item.subtotal, 0);
    const discountAmount = random.chance(0.15) ? roundTo(subtotal * random.pick([0.05, 0.1]), 500) : 0;
    const total = subtotal - discountAmount;
    const paymentMethod = random.pick(['CASH', 'CASH', 'CASH', 'TRANSFER', 'TRANSFER', 'CARD']);
    const receivedAmount =
      paymentMethod === 'CASH' ? Math.ceil(total / 10000) * 10000 + (random.chance(0.3) ? 50000 : 0) : total;
    dayCounters[dateKey] = (dayCounters[dateKey] || 0) + 1;
    const code = `HD-${dateKey.replaceAll('-', '')}-${pad(dayCounters[dateKey], 4)}`;

    const invoice = {
      id: `inv-${pad(invoices.length + 1, 4)}`,
      code,
      status: cancelled ? 'CANCELLED' : 'COMPLETED',
      createdAt: date.toISOString(),
      employeeId: employee.id,
      employeeName: employee.name,
      customerId: customer ? customer.id : null,
      customerName: customer ? customer.name : 'Khách lẻ',
      items,
      subtotal,
      discountAmount,
      total,
      paymentMethod,
      receivedAmount,
      changeAmount: receivedAmount - total,
      prescriptionCode: needsCustomer ? `DT-${pad(random.int(1, 99999), 5)}` : '',
      note: cancelled ? 'Khách đổi ý, hủy đơn' : '',
      cancelReason: cancelled ? 'Khách đổi ý, hủy đơn' : '',
    };
    invoices.push(invoice);

    if (cancelled) {
      applied.forEach(({ batch, quantity }) => {
        batch.remainingQty += quantity;
      });
      invoice.items.forEach((item) => {
        item.allocations = [];
      });
    } else {
      applied.forEach(({ batch, quantity, medicine }) => {
        movements.push({
          createdAt: invoice.createdAt,
          type: 'OUT',
          medicineId: medicine.id,
          medicineName: medicine.name,
          batchId: batch.id,
          batchCode: batch.code,
          quantity: -quantity,
          referenceType: 'INVOICE',
          referenceCode: code,
          performedById: employee.id,
          performedByName: employee.name,
          note: 'Bán tại quầy',
        });
      });
    }
  });

  return { invoices, movements };
};

const buildAdjustments = (random, context) => {
  const { medicines, batches, employees, now } = context;
  const keeper = employees.find((employee) => employee.role === 'WAREHOUSE');
  const todayKey = toDateKey(now);
  const movements = [];
  const stamp = () => addDays(now, 0).toISOString();
  const adjust = (batch, newQty, type, note) => {
    const delta = newQty - batch.remainingQty;
    if (delta === 0) return;
    const medicine = medicines.find((item) => item.id === batch.medicineId);
    batch.remainingQty = newQty;
    movements.push({
      createdAt: new Date(new Date(stamp()).getTime() - random.int(2, 120) * 60000).toISOString(),
      type,
      medicineId: medicine.id,
      medicineName: medicine.name,
      batchId: batch.id,
      batchCode: batch.code,
      quantity: delta,
      referenceType: type === 'WRITE_OFF' ? 'WRITE_OFF' : 'STOCKTAKE',
      referenceCode: type === 'WRITE_OFF' ? 'HUY-HET-HAN' : 'KK-DIEU-CHINH',
      performedById: keeper.id,
      performedByName: keeper.name,
      note,
    });
  };

  const byMedicine = new Map();
  batches.forEach((batch) => {
    if (!byMedicine.has(batch.medicineId)) byMedicine.set(batch.medicineId, []);
    byMedicine.get(batch.medicineId).push(batch);
  });
  const candidates = random.shuffle(medicines.filter((medicine) => medicine.status === 'ACTIVE'));

  candidates.slice(0, 14).forEach((medicine) => {
    byMedicine.get(medicine.id).forEach((batch) => adjust(batch, 0, 'ADJUSTMENT', 'Điều chỉnh sau kiểm kê: hết hàng'));
  });
  candidates.slice(14, 54).forEach((medicine) => {
    const list = byMedicine.get(medicine.id);
    const target = random.int(1, Math.max(1, medicine.minStock - 1));
    list.forEach((batch, index) =>
      adjust(
        batch,
        index === 0 ? Math.min(batch.remainingQty, target) : 0,
        'ADJUSTMENT',
        'Điều chỉnh sau kiểm kê: tồn thấp',
      ),
    );
  });

  const expired = batches.filter((batch) => batch.expiryDate < todayKey && batch.remainingQty > 0);
  expired
    .slice(0, Math.max(0, Math.floor(expired.length / 3)))
    .forEach((batch) => adjust(batch, 0, 'WRITE_OFF', 'Hủy lô hết hạn'));

  return movements;
};

const buildStocktakes = (random, context) => {
  const { medicines, batches, employees, now } = context;
  const keeper = employees.find((employee) => employee.role === 'WAREHOUSE');
  const admin = employees.find((employee) => employee.role === 'ADMIN');
  const medicineById = Object.fromEntries(medicines.map((medicine) => [medicine.id, medicine]));
  const makeLines = (count, offset, withActual, withDiff) =>
    batches
      .filter((batch) => batch.remainingQty > 0)
      .slice(offset, offset + count)
      .map((batch) => {
        const diff = withDiff && random.chance(0.35) ? -random.int(1, Math.min(3, batch.remainingQty)) : 0;
        const actualQty = withActual ? batch.remainingQty + diff : null;
        return {
          batchId: batch.id,
          batchCode: batch.code,
          medicineId: batch.medicineId,
          medicineName: medicineById[batch.medicineId].name,
          systemQty: batch.remainingQty,
          actualQty,
          difference: actualQty === null ? 0 : actualQty - batch.remainingQty,
          reason: diff ? 'Hao hụt khi sắp xếp kho' : '',
        };
      });

  return [
    {
      id: 'stk-001',
      code: 'KK-2026-001',
      status: 'APPROVED',
      scope: 'ALL',
      scopeLabel: 'Toàn bộ kho (mẫu)',
      note: 'Kiểm kê định kỳ quý trước',
      createdAt: addDays(now, -40).toISOString(),
      createdById: keeper.id,
      createdByName: keeper.name,
      submittedAt: addDays(now, -39).toISOString(),
      reviewedById: admin.id,
      reviewedByName: admin.name,
      reviewedAt: addDays(now, -38).toISOString(),
      reviewNote: 'Số liệu khớp, đã duyệt',
      lines: makeLines(12, 0, true, false),
    },
    {
      id: 'stk-002',
      code: 'KK-2026-002',
      status: 'PENDING_APPROVAL',
      scope: 'ALL',
      scopeLabel: 'Kệ A - nhóm tim mạch',
      note: 'Kiểm kê đột xuất khu thuốc kê đơn',
      createdAt: addDays(now, -2).toISOString(),
      createdById: keeper.id,
      createdByName: keeper.name,
      submittedAt: addDays(now, -1).toISOString(),
      reviewedById: null,
      reviewedByName: null,
      reviewedAt: null,
      reviewNote: '',
      lines: makeLines(10, 40, true, true),
    },
    {
      id: 'stk-003',
      code: 'KK-2026-003',
      status: 'DRAFT',
      scope: 'ALL',
      scopeLabel: 'Kệ B - vitamin',
      note: '',
      createdAt: addDays(now, -1).toISOString(),
      createdById: keeper.id,
      createdByName: keeper.name,
      submittedAt: null,
      reviewedById: null,
      reviewedByName: null,
      reviewedAt: null,
      reviewNote: '',
      lines: makeLines(8, 80, false, false),
    },
  ];
};

export const generateSeed = (now = new Date()) => {
  const random = createRandom(20260427);
  const categories = buildCategories();
  const suppliers = buildSuppliers(random, now);
  const medicines = buildMedicines(random, categories, now);
  const employees = buildEmployees(now);
  const customers = buildCustomers(random, now);
  const {
    receipts,
    batches,
    movements: receiptMovements,
  } = buildReceiptsAndBatches(random, { medicines, suppliers, employees, now });
  const { invoices, movements: saleMovements } = buildInvoices(random, {
    medicines,
    customers,
    employees,
    batches,
    now,
  });
  const adjustmentMovements = buildAdjustments(random, { medicines, batches, employees, now });
  const stocktakes = buildStocktakes(random, { medicines, batches, employees, now });

  const movements = [...receiptMovements, ...saleMovements, ...adjustmentMovements]
    .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    .map((movement, index) => ({ id: `mov-${pad(index + 1, 5)}`, ...movement }));

  const admin = employees[0];
  const activityLogs = [
    ...invoices.slice(-14).map((invoice) => ({
      createdAt: invoice.createdAt,
      actorId: invoice.employeeId,
      actorName: invoice.employeeName,
      role: 'SALES',
      action: 'CREATE_INVOICE',
      entity: 'INVOICE',
      entityCode: invoice.code,
      description: `Tạo hóa đơn ${invoice.code}`,
    })),
    ...receipts
      .filter((receipt) => receipt.status === 'CONFIRMED')
      .slice(-8)
      .map((receipt) => ({
        createdAt: receipt.confirmedAt,
        actorId: receipt.createdById,
        actorName: receipt.createdByName,
        role: 'WAREHOUSE',
        action: 'CONFIRM_RECEIPT',
        entity: 'RECEIPT',
        entityCode: receipt.code,
        description: `Xác nhận phiếu nhập ${receipt.code}`,
      })),
    {
      createdAt: addDays(now, -38).toISOString(),
      actorId: admin.id,
      actorName: admin.name,
      role: 'ADMIN',
      action: 'APPROVE_STOCKTAKE',
      entity: 'STOCKTAKE',
      entityCode: 'KK-2026-001',
      description: 'Duyệt phiếu kiểm kê KK-2026-001',
    },
  ]
    .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    .map((log, index) => ({ id: `log-${pad(index + 1, 4)}`, ...log }));

  const invitations = [
    {
      id: 'inv-01',
      code: 'NV-SALES-2026',
      email: 'nhanvien.moi@pharmacy.demo',
      role: 'SALES',
      status: 'PENDING',
      expiresAt: addDays(now, 14).toISOString(),
      createdByName: admin.name,
      createdAt: now.toISOString(),
    },
    {
      id: 'inv-02',
      code: 'NV-KHO-2026',
      email: 'thukho.moi@pharmacy.demo',
      role: 'WAREHOUSE',
      status: 'PENDING',
      expiresAt: addDays(now, 14).toISOString(),
      createdByName: admin.name,
      createdAt: now.toISOString(),
    },
    {
      id: 'inv-03',
      code: 'HET-HAN-0001',
      email: 'cu@pharmacy.demo',
      role: 'SALES',
      status: 'EXPIRED',
      expiresAt: addDays(now, -3).toISOString(),
      createdByName: admin.name,
      createdAt: addDays(now, -20).toISOString(),
    },
  ];

  const settings = {
    pharmacy: {
      name: 'Nhà thuốc Minh Tâm',
      address: '125 Nguyễn Trãi, Quận 1, TP. Hồ Chí Minh',
      phone: '028 3822 1234',
      email: 'lienhe@minhtam.vn',
      taxCode: '0312345678',
      license: 'GPP-2024-0457',
    },
    lowStockMultiplier: 1,
    expiryWarningDays: 90,
    expiryTiers: [30, 60, 90],
    appearance: { theme: 'light', density: 'comfortable' },
    notifications: { lowStock: true, expiring: true, newReceipt: true, dailyReport: false },
  };

  return {
    categories,
    suppliers,
    medicines,
    customers,
    employees,
    receipts,
    batches,
    invoices,
    movements,
    stocktakes,
    activityLogs,
    invitations,
    drafts: [],
    settings,
  };
};
