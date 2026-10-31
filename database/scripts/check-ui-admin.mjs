import puppeteer from "puppeteer-core";

const executablePath =
  process.env.BROWSER_PATH ||
  "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const baseUrl = process.env.BASE_URL || "http://localhost:8088";
const password = process.env.ADMIN_PASSWORD || "Pharmacy@123";
const newPassword = "MatKhau@12345";
const suffix = String(Date.now()).slice(-6);
const pages = [
  "dashboard",
  "medicines",
  "categories",
  "inventory",
  "disposals",
  "receipts",
  "suppliers",
  "invoices",
  "customers",
  "employees",
  "reports",
  "activity",
  "settings",
];

const pause = (milliseconds) =>
  new Promise((resolveWait) => setTimeout(resolveWait, milliseconds));
const problems = [];
const record = (message) => {
  problems.push(message);
  process.stdout.write(`  PROBLEM ${message}\n`);
};

const watch = (page, label) => {
  page.on("response", (response) => {
    const url = response.url();
    if (url.includes("/api/v1/") && response.status() >= 400) {
      const known =
        (response.status() === 401 && url.includes("/auth/me")) ||
        (response.status() === 409 && url.includes("/auth/activation/"));
      if (!known)
        record(
          `${label}: ${response.request().method()} ${url.split("/api/v1")[1]} -> ${response.status()}`,
        );
    }
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

const login = async (page, identifier, secret) => {
  await page.goto(`${baseUrl}/pages/auth/login.html`, {
    waitUntil: "networkidle2",
  });
  await page.type("#identifier", identifier);
  await page.type("#password", secret);
  await Promise.all([
    page
      .waitForNavigation({ waitUntil: "networkidle2", timeout: 30000 })
      .catch(() => null),
    page.click('button[type="submit"]'),
  ]);
  await pause(1500);
};

const textOf = (page, selector) =>
  page.$eval(selector, (element) => element.innerText).catch(() => "");

const browser = await puppeteer.launch({
  executablePath,
  headless: true,
  args: ["--no-sandbox"],
});
try {
  const context = await browser.createBrowserContext();
  const page = await context.newPage();
  watch(page, "admin");
  await login(page, "admin", password);
  process.stdout.write(`admin landed on ${new URL(page.url()).pathname}\n`);

  for (const name of pages) {
    await page.goto(`${baseUrl}/pages/admin/${name}.html`, {
      waitUntil: "networkidle2",
    });
    await pause(1200);
    const rows = await page
      .$$eval("tbody tr", (elements) => elements.length)
      .catch(() => 0);
    const alerts = await page.$$eval(".alert--danger", (elements) =>
      elements.map((element) => element.innerText),
    );
    process.stdout.write(
      `${name}: rows=${rows}${alerts.length ? ` alerts=${alerts.join("|")}` : ""}\n`,
    );
    if (alerts.length) record(`${name}: ${alerts.join("|")}`);
  }

  await page.goto(`${baseUrl}/pages/admin/employees.html`, {
    waitUntil: "networkidle2",
  });
  await page.click('[data-page-action="create"]');
  await pause(500);
  const username = `nv.test${suffix}`;
  await page.type('.modal [name="name"]', "Nhân viên kiểm thử");
  await page.type('.modal [name="email"]', `${username}@nhathuoc.vn`);
  await page.type('.modal [name="username"]', username);
  await page.select('.modal [name="role"]', "SALES");
  await page.click(".modal [data-submit]");
  await pause(1500);
  const activationLink = await page
    .$eval(".modal [data-link]", (input) => input.value)
    .catch(() => "");
  process.stdout.write(
    `activation link: ${activationLink ? "shown" : "MISSING"}\n`,
  );
  if (!activationLink.includes("activate.html?token="))
    record("activation link not shown");

  const employeeContext = await browser.createBrowserContext();
  const employeePage = await employeeContext.newPage();
  watch(employeePage, "new-employee");
  await employeePage.goto(
    activationLink.replace("localhost:8088", new URL(baseUrl).host),
    { waitUntil: "networkidle2" },
  );
  await pause(800);
  await employeePage.type("#act-password", newPassword);
  await employeePage.type("#act-confirmPassword", newPassword);
  await employeePage.click("[data-submit]");
  await pause(1500);
  const activated = (await textOf(employeePage, "[data-auth-card]")).includes(
    "Kích hoạt thành công",
  );
  process.stdout.write(`activation: ${activated ? "ok" : "FAILED"}\n`);
  if (!activated) record("activation did not succeed");
  await employeePage.goto(activationLink, { waitUntil: "networkidle2" });
  await pause(800);
  const reusable = !(await textOf(employeePage, "[data-auth-card]")).includes(
    "không hợp lệ",
  );
  if (reusable) record("activation link reusable");
  await login(employeePage, username, newPassword);
  const landed = new URL(employeePage.url()).pathname;
  process.stdout.write(`new employee landed on ${landed}\n`);
  if (landed !== "/pages/sales/pos.html")
    record(`new employee landed on ${landed}`);

  await page.goto(`${baseUrl}/pages/admin/disposals.html`, {
    waitUntil: "networkidle2",
  });
  await pause(1000);
  const pendingRow = await page.$$eval("tbody tr", (rows) =>
    rows.findIndex((row) => row.innerText.includes("Chờ duyệt")),
  );
  process.stdout.write(`pending disposal row index: ${pendingRow}\n`);
  if (pendingRow >= 0) {
    await page.evaluate(
      (index) => document.querySelectorAll("tbody tr")[index].click(),
      pendingRow,
    );
    await page.waitForSelector('.drawer [data-act="approve"]', {
      timeout: 5000,
    });
    await page.click('.drawer [data-act="approve"]');
    await pause(500);
    await page.evaluate(() => {
      const button = [...document.querySelectorAll(".modal .btn")].find(
        (item) => item.innerText.includes("Duyệt phiếu"),
      );
      button.click();
    });
    await pause(1500);
    const status = await textOf(page, ".drawer");
    process.stdout.write(
      `disposal approval: ${status.includes("Đã duyệt") ? "ok" : "FAILED"}\n`,
    );
    if (!status.includes("Đã duyệt")) record("disposal not approved");
  }
} finally {
  await browser.close();
}
process.stdout.write(
  problems.length ? `${problems.length} problem(s)\n` : "all checks passed\n",
);
process.exit(problems.length ? 1 : 0);
