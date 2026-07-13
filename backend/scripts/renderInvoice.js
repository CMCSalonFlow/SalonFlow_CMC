const fs = require("fs");
const path = require("path");

const puppeteer = require("puppeteer");
const Handlebars = require("handlebars");

(async () => {

    try {

        // Đọc dữ liệu truyền vào
        const jsonPath = process.argv[2];

        const outputPath = process.argv[3];

        if (!jsonPath || !outputPath) {
            throw new Error("Thiếu tham số.");
        }

        const data = JSON.parse(
            fs.readFileSync(jsonPath, "utf8")
        );

        // Đọc template
        const templatePath = path.join(
            __dirname,
            "../src/main/resources/templates/invoice/invoice.hbs"
        );

        const templateSource =
            fs.readFileSync(templatePath, "utf8");

        const template =
            Handlebars.compile(templateSource);

        const html = template(data);

        // Mở Chrome Headless
        const browser = await puppeteer.launch({

            headless: true

        });

        const page = await browser.newPage();

        await page.setContent(html, {

            waitUntil: "networkidle0"

        });

        await page.pdf({

            path: outputPath,

            format: "A4",

            printBackground: true

        });

        await browser.close();

        console.log("PDF created:", outputPath);

    } catch (err) {

        console.error(err);

        process.exit(1);

    }

})();