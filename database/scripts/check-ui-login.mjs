import puppeteer from "puppeteer-core";

const executablePath =
  process.env.BROWSER_PATH ||
  "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const baseUrl = process.env.BASE_URL || "http://localhost:8088";
const password = process.env.ADMIN_PASSWORD || "Pharmacy@123";
const accounts = [{ identifier: "admin", home: "/pages/admin/dashboard.html" }];

const browser = await puppeteer.launch({
  executablePath,
  headless: true,
  args: ["--no-sandbox"],
});
let failures = 0;
try {
  for (const account of accounts) {
    const context = await browser.createBrowserContext();
    const page = await context.newPage();
    const apiCalls = [];
    page.on("response", (response) => {
      const url = response.url();
      if (url.includes("/api/v1/"))
        apiCalls.push(
          `${response.request().method()} ${url.split("/api/v1")[1]} ${response.status()}`,
        );
    });
    await page.goto(`${baseUrl}/pages/auth/login.html`, {
      waitUntil: "networkidle2",
    });
    await page.type("#identifier", account.identifier);
    await page.type("#password", password);
    await Promise.all([
      page
        .waitForNavigation({ waitUntil: "networkidle2", timeout: 30000 })
        .catch(() => null),
      page.click('button[type="submit"]'),
    ]);
    await new Promise((resolveWait) => setTimeout(resolveWait, 2500));
    const location = new URL(page.url()).pathname;
    const loggedIn = location === account.home;
    const notifications = apiCalls.filter((call) =>
      call.includes("/notifications"),
    );
    const settings = apiCalls.filter((call) => call.includes("/settings"));
    const failed = apiCalls.filter((call) => /\s(4|5)\d\d$/.test(call));
    process.stdout.write(
      `${account.identifier}: landed=${location} ok=${loggedIn} notifications=${notifications.join(",") || "none"} settings=${settings.join(",") || "none"}\n`,
    );
    if (failed.length)
      process.stdout.write(`  failing calls: ${failed.join(" | ")}\n`);
    if (!loggedIn) failures += 1;
    await context.close();
  }
} finally {
  await browser.close();
}
process.exit(failures ? 1 : 0);
