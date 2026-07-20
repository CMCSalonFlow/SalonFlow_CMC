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

        const template = Handlebars.compile(templateSource);

        const normalizedData = {
            ...data,
            salonName: data.salonName || "SalonFlow",
            salonAddress: data.salonAddress || "",
            salonPhone: data.salonPhone || "",
            bookingCode: data.bookingCode || ("#BK" + (data.bookingId || "")),
            customerName: data.customerName || "Khách hàng",
            customerPhone: data.customerPhone || "",
            bookingDate: data.bookingDate || (data.bookingTime ? data.bookingTime.toString().replace("T", " ") : ""),
            grandTotal: (data.total || data.grandTotal || 0).toLocaleString("vi-VN") + " VND",
            subTotal: (data.subTotal || 0).toLocaleString("vi-VN") + " VND",
            tax: (data.tax || 0).toLocaleString("vi-VN") + " VND",
            services: (data.items || data.services || []).map(item => ({
                name: item.serviceName || item.name || "Dịch vụ",
                quantity: item.quantity || 1,
                price: (item.unitPrice || item.price || 0).toLocaleString("vi-VN") + " VND",
                total: (item.totalPrice || item.total || 0).toLocaleString("vi-VN") + " VND"
            }))
        };

        const html = template(normalizedData);

        // Mở Chrome Headless
        const browser = await puppeteer.launch({
            headless: true,
            args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage']
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