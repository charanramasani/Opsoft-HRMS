package com.opsoft.hrms_backend.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.opsoft.hrms_backend.dto.PayrollComponentResponse;
import com.opsoft.hrms_backend.dto.PayrollResponse;

@Service
public class PayslipPdfService {

    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float LEFT = 38;
    private static final float RIGHT = PAGE_WIDTH - 38;
    private static final float CONTENT_WIDTH = RIGHT - LEFT;

    private static final PDColor NAVY = color(20, 39, 66);
    private static final PDColor TEAL = color(0, 137, 123);
    private static final PDColor PALE_TEAL = color(232, 248, 244);
    private static final PDColor LIGHT_BLUE = color(242, 246, 250);
    private static final PDColor LIGHT_GREY = color(247, 248, 250);
    private static final PDColor MID_GREY = color(215, 222, 230);
    private static final PDColor DARK = color(43, 52, 62);
    private static final PDColor GREY = color(105, 116, 128);
    private static final PDColor WHITE = color(255, 255, 255);

    private static final PDType1Font BOLD =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font REGULAR =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private static final DecimalFormat AMOUNT_FORMAT =
            new DecimalFormat("#,##0.00");

    public byte[] generatePayslip(
            PayrollResponse payroll,
            List<PayrollComponentResponse> components) throws IOException {

        if (payroll == null) {
            throw new IllegalArgumentException("Payroll details are required.");
        }

        List<PayrollComponentResponse> earnings = new ArrayList<>();
        List<PayrollComponentResponse> deductions = new ArrayList<>();

        if (components != null) {
            for (PayrollComponentResponse component : components) {
                if (component == null) {
                    continue;
                }
                if ("EARNING".equalsIgnoreCase(component.getComponentType())) {
                    earnings.add(component);
                } else if ("DEDUCTION".equalsIgnoreCase(component.getComponentType())) {
                    deductions.add(component);
                }
            }
        }

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream stream =
                         new PDPageContentStream(document, page)) {
                drawHeader(document, stream, payroll);
                float employeeBottom = drawEmployeeSummary(stream, payroll);
                float tableBottom = drawSalaryComponents(
                        stream, earnings, deductions, payroll, employeeBottom);
                drawSalaryTotals(stream, payroll, tableBottom);
                drawFooter(stream);
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private void drawHeader(
            PDDocument document,
            PDPageContentStream stream,
            PayrollResponse payroll) throws IOException {

        float headerY = 755;
        float headerHeight = 52;

        fillRect(stream, LEFT, headerY, CONTENT_WIDTH, headerHeight, NAVY);

        // Logo is optional at runtime; the text brand remains as a fallback.
        boolean logoDrawn = drawCompanyLogo(document, stream,
                LEFT + 14, headerY + 10, 142, 32);

        if (!logoDrawn) {
            drawText(stream, BOLD, 19, WHITE, "OPSOFT INC",
                    LEFT + 16, headerY + 29);
            drawText(stream, REGULAR, 7, color(218, 230, 239),
                    "HUMAN RESOURCE MANAGEMENT",
                    LEFT + 17, headerY + 15);
        }

        drawTextRight(stream, BOLD, 15, WHITE,
                "SALARY SLIP", RIGHT - 15, headerY + 30);

        String month = payroll.getPayrollMonth() == null
                ? "-"
                : payroll.getPayrollMonth().format(
                        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));

        drawTextRight(stream, REGULAR, 9, color(218, 230, 239),
                "Pay period: " + month, RIGHT - 15, headerY + 14);

        drawText(stream, REGULAR, 7, GREY,
                "Building No. 9, 13th Floor, Raheja Mindspace, HITEC City, Hyderabad - 500081, Telangana, India",
                LEFT, 742);

        drawText(stream, BOLD, 10, NAVY,
                "EMPLOYEE PAYSLIP", LEFT, 727);
        drawTextRight(stream, REGULAR, 8, GREY,
                "Payment status: " + safe(payroll.getPaymentStatus()),
                RIGHT, 727);
    }

    private boolean drawCompanyLogo(
            PDDocument document,
            PDPageContentStream stream,
            float x,
            float y,
            float maxWidth,
            float maxHeight) {

        try (InputStream input =
                     new ClassPathResource("opsoft-logo.png").getInputStream()) {

            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                return false;
            }

            PDImageXObject logo = LosslessFactory.createFromImage(document, image);
            float scale = Math.min(maxWidth / logo.getWidth(),
                    maxHeight / logo.getHeight());
            float width = logo.getWidth() * scale;
            float height = logo.getHeight() * scale;

            stream.drawImage(logo, x, y + (maxHeight - height) / 2,
                    width, height);
            return true;

        } catch (IOException | RuntimeException exception) {
            // Missing/invalid logo should not prevent payslip generation.
            return false;
        }
    }

    private float drawEmployeeSummary(
            PDPageContentStream stream,
            PayrollResponse payroll) throws IOException {

        float top = 720;
        float height = 119;
        float bottom = top - height;
        float leftWidth = 320;
        float gap = 10;
        float rightWidth = CONTENT_WIDTH - leftWidth - gap;
        float rightX = LEFT + leftWidth + gap;

        fillRect(stream, LEFT, bottom, leftWidth, height, LIGHT_BLUE);
        strokeRect(stream, LEFT, bottom, leftWidth, height, MID_GREY);
        fillRect(stream, rightX, bottom, rightWidth, height, PALE_TEAL);
        strokeRect(stream, rightX, bottom, rightWidth, height, MID_GREY);

        drawText(stream, BOLD, 9, NAVY,
                "EMPLOYEE INFORMATION", LEFT + 12, top - 18);

        float labelX = LEFT + 12;
        float valueX = LEFT + 112;
        float rowY = top - 39;

        drawDetail(stream, labelX, valueX, rowY, "Employee ID",
                safe(payroll.getEmployeeId()));
        rowY -= 17;
        drawDetail(stream, labelX, valueX, rowY, "Employee Name",
                safe(payroll.getEmployeeName()));
        rowY -= 17;
        drawDetail(stream, labelX, valueX, rowY, "Designation",
                safe(payroll.getDesignation()));
        rowY -= 17;

        String joiningDate = payroll.getJoiningDate() == null
                ? "-"
                : payroll.getJoiningDate().format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
        drawDetail(stream, labelX, valueX, rowY, "Date of Joining", joiningDate);
        rowY -= 17;

        String month = payroll.getPayrollMonth() == null
                ? "-"
                : payroll.getPayrollMonth().format(
                        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
        drawDetail(stream, labelX, valueX, rowY, "Pay Period", month);

        drawText(stream, BOLD, 8, TEAL,
                "NET PAYABLE", rightX + 13, top - 25);
        drawText(stream, BOLD, 16, NAVY,
                "Rs. " + money(payroll.getNetSalary()),
                rightX + 13, top - 55);
        drawText(stream, REGULAR, 7, GREY,
                "Take-home salary", rightX + 13, top - 74);

        drawText(stream, REGULAR, 7, GREY,
                "Status: " + safe(payroll.getPaymentStatus()),
                rightX + 13, bottom + 14);

        return bottom;
    }

    private void drawDetail(
            PDPageContentStream stream,
            float labelX,
            float valueX,
            float y,
            String label,
            String value) throws IOException {

        drawText(stream, REGULAR, 8, GREY, label, labelX, y);
        drawText(stream, BOLD, 8, DARK, truncate(value, 32), valueX, y);
    }

    private float drawSalaryComponents(
            PDPageContentStream stream,
            List<PayrollComponentResponse> earnings,
            List<PayrollComponentResponse> deductions,
            PayrollResponse payroll,
            float employeeBottom) throws IOException {

        float top = employeeBottom - 18;
        float half = CONTENT_WIDTH / 2;
        float headerHeight = 25;
        float rowHeight = 21;
        float totalHeight = 25;
        int rows = Math.max(1, Math.max(earnings.size(), deductions.size()));

        fillRect(stream, LEFT, top - headerHeight,
                CONTENT_WIDTH, headerHeight, NAVY);

        float leftNameX = LEFT + 10;
        float leftAmountRight = LEFT + half - 10;
        float rightNameX = LEFT + half + 10;
        float rightAmountRight = RIGHT - 10;

        drawText(stream, BOLD, 8, WHITE, "EARNINGS",
                leftNameX, top - 16);
        drawTextRight(stream, BOLD, 8, WHITE, "AMOUNT",
                leftAmountRight, top - 16);
        drawText(stream, BOLD, 8, WHITE, "DEDUCTIONS",
                rightNameX, top - 16);
        drawTextRight(stream, BOLD, 8, WHITE, "AMOUNT",
                rightAmountRight, top - 16);

        float currentTop = top - headerHeight;

        for (int i = 0; i < rows; i++) {
            float rowBottom = currentTop - rowHeight;
            fillRect(stream, LEFT, rowBottom, CONTENT_WIDTH, rowHeight,
                    i % 2 == 0 ? WHITE : LIGHT_GREY);

            if (i < earnings.size()) {
                PayrollComponentResponse item = earnings.get(i);
                drawText(stream, REGULAR, 8, DARK,
                        truncate(displayName(item.getComponentName()), 28),
                        leftNameX, currentTop - 14);
                drawTextRight(stream, REGULAR, 8, DARK,
                        money(item.getAmount()), leftAmountRight, currentTop - 14);
            }

            if (i < deductions.size()) {
                PayrollComponentResponse item = deductions.get(i);
                drawText(stream, REGULAR, 8, DARK,
                        truncate(displayName(item.getComponentName()), 28),
                        rightNameX, currentTop - 14);
                drawTextRight(stream, REGULAR, 8, DARK,
                        money(item.getAmount()), rightAmountRight, currentTop - 14);
            }

            currentTop = rowBottom;
        }

        float totalBottom = currentTop - totalHeight;
        fillRect(stream, LEFT, totalBottom,
                CONTENT_WIDTH, totalHeight, LIGHT_BLUE);

        drawText(stream, BOLD, 8, NAVY,
                "GROSS EARNINGS", leftNameX, currentTop - 16);
        drawTextRight(stream, BOLD, 8, NAVY,
                "Rs. " + money(payroll.getGrossSalary()),
                leftAmountRight, currentTop - 16);
        drawText(stream, BOLD, 8, NAVY,
                "TOTAL DEDUCTIONS", rightNameX, currentTop - 16);
        drawTextRight(stream, BOLD, 8, NAVY,
                "Rs. " + money(payroll.getDeductions()),
                rightAmountRight, currentTop - 16);

        strokeRect(stream, LEFT, totalBottom,
                CONTENT_WIDTH, top - totalBottom, MID_GREY);
        drawLine(stream, LEFT + half, totalBottom,
                LEFT + half, top, MID_GREY, 0.7f);

        return totalBottom;
    }

    private void drawSalaryTotals(
            PDPageContentStream stream,
            PayrollResponse payroll,
            float tableBottom) throws IOException {

        float panelTop = tableBottom - 18;
        float panelHeight = 48;
        float panelBottom = panelTop - panelHeight;

        fillRect(stream, LEFT, panelBottom,
                CONTENT_WIDTH, panelHeight, PALE_TEAL);
        strokeRect(stream, LEFT, panelBottom,
                CONTENT_WIDTH, panelHeight, MID_GREY);

        drawText(stream, BOLD, 9, NAVY,
                "TOTAL NET PAYABLE", LEFT + 13, panelTop - 18);
        drawText(stream, REGULAR, 7, GREY,
                "Gross earnings less total deductions",
                LEFT + 13, panelTop - 32);
        drawTextRight(stream, BOLD, 15, TEAL,
                "Rs. " + money(payroll.getNetSalary()),
                RIGHT - 13, panelTop - 29);

        // Larger, clearly separated amount-in-words section.
        float wordsTop = panelBottom - 12;
        drawText(stream, BOLD, 8, NAVY,
                "AMOUNT IN WORDS", LEFT, wordsTop);

        String amountInWords = amountToIndianWords(payroll.getNetSalary());
        List<String> wordsLines = wrapText(
                amountInWords, REGULAR, 10, CONTENT_WIDTH - 8);

        float wordsY = wordsTop - 17;
        for (String line : wordsLines) {
            drawText(stream, REGULAR, 10, DARK, line, LEFT, wordsY);
            wordsY -= 14;
        }
    }

    private List<String> wrapText(
            String text,
            PDType1Font font,
            float fontSize,
            float maxWidth) throws IOException {

        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : text.split("\\s+")) {
            String candidate = currentLine.length() == 0
                    ? word
                    : currentLine + " " + word;

            float candidateWidth =
                    font.getStringWidth(pdfSafe(candidate)) / 1000f * fontSize;

            if (candidateWidth > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) {
                    currentLine.append(' ');
                }
                currentLine.append(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private String amountToIndianWords(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        BigDecimal rounded = amount.abs().setScale(2, java.math.RoundingMode.HALF_UP);
        java.math.BigInteger rupees = rounded.toBigInteger();
        int paise = rounded.remainder(BigDecimal.ONE)
                .movePointRight(2).intValue();

        String words = indianNumberWords(rupees) + " Rupees";
        if (paise > 0) {
            words += " and " + belowThousandWords(paise) + " Paise";
        }

        if (amount.signum() < 0) {
            words = "Minus " + words;
        }

        return words + " Only";
    }

    private String indianNumberWords(java.math.BigInteger number) {
        if (number.signum() == 0) {
            return "Zero";
        }

        java.math.BigInteger crore = java.math.BigInteger.valueOf(10000000);
        java.math.BigInteger lakh = java.math.BigInteger.valueOf(100000);
        java.math.BigInteger thousand = java.math.BigInteger.valueOf(1000);

        List<String> parts = new ArrayList<>();
        java.math.BigInteger[] result;

        result = number.divideAndRemainder(crore);
        if (result[0].signum() > 0) {
            parts.add(indianNumberWords(result[0]) + " Crore");
        }
        number = result[1];

        result = number.divideAndRemainder(lakh);
        if (result[0].signum() > 0) {
            parts.add(belowThousandWords(result[0].intValue()) + " Lakh");
        }
        number = result[1];

        result = number.divideAndRemainder(thousand);
        if (result[0].signum() > 0) {
            parts.add(belowThousandWords(result[0].intValue()) + " Thousand");
        }
        number = result[1];

        if (number.signum() > 0) {
            parts.add(belowThousandWords(number.intValue()));
        }

        return String.join(" ", parts);
    }

    private String belowThousandWords(int number) {
        String[] ones = {
                "Zero", "One", "Two", "Three", "Four", "Five", "Six",
                "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve",
                "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
                "Eighteen", "Nineteen"
        };
        String[] tens = {
                "", "", "Twenty", "Thirty", "Forty", "Fifty",
                "Sixty", "Seventy", "Eighty", "Ninety"
        };

        if (number < 20) {
            return ones[number];
        }

        if (number < 100) {
            int remainder = number % 10;
            return tens[number / 10] + (remainder == 0 ? "" : " " + ones[remainder]);
        }

        int remainder = number % 100;
        return ones[number / 100] + " Hundred"
                + (remainder == 0 ? "" : " " + belowThousandWords(remainder));
    }

    private void drawFooter(PDPageContentStream stream) throws IOException {
        drawLine(stream, LEFT, 43, RIGHT, 43, MID_GREY, 0.7f);
        drawText(stream, REGULAR, 7, GREY,
                "This is a system-generated payslip issued by Opsoft Inc.",
                LEFT, 29);
        drawTextRight(stream, REGULAR, 7, GREY,
                "No signature is required.", RIGHT, 29);
    }

    private void drawText(
            PDPageContentStream stream,
            PDType1Font font,
            float size,
            PDColor textColor,
            String text,
            float x,
            float y) throws IOException {

        stream.beginText();
        stream.setFont(font, size);
        stream.setNonStrokingColor(textColor);
        stream.newLineAtOffset(x, y);
        stream.showText(pdfSafe(text));
        stream.endText();
    }

    private void drawTextRight(
            PDPageContentStream stream,
            PDType1Font font,
            float size,
            PDColor textColor,
            String text,
            float rightX,
            float y) throws IOException {

        String safeText = pdfSafe(text);
        float textWidth = font.getStringWidth(safeText) / 1000f * size;
        drawText(stream, font, size, textColor, safeText,
                rightX - textWidth, y);
    }

    private void fillRect(
            PDPageContentStream stream,
            float x,
            float y,
            float width,
            float height,
            PDColor fill) throws IOException {

        stream.setNonStrokingColor(fill);
        stream.addRect(x, y, width, height);
        stream.fill();
    }

    private void strokeRect(
            PDPageContentStream stream,
            float x,
            float y,
            float width,
            float height,
            PDColor stroke) throws IOException {

        stream.setStrokingColor(stroke);
        stream.setLineWidth(0.7f);
        stream.addRect(x, y, width, height);
        stream.stroke();
    }

    private void drawLine(
            PDPageContentStream stream,
            float x1,
            float y1,
            float x2,
            float y2,
            PDColor stroke,
            float width) throws IOException {

        stream.setStrokingColor(stroke);
        stream.setLineWidth(width);
        stream.moveTo(x1, y1);
        stream.lineTo(x2, y2);
        stream.stroke();
    }

    private static PDColor color(float red, float green, float blue) {
        return new PDColor(new float[] {
                red / 255f, green / 255f, blue / 255f
        }, PDDeviceRGB.INSTANCE);
    }

    private String displayName(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        String normalized = value.replace('_', ' ').toLowerCase(Locale.ENGLISH);
        StringBuilder result = new StringBuilder();
        boolean capitalize = true;

        for (char character : normalized.toCharArray()) {
            if (Character.isWhitespace(character)) {
                result.append(character);
                capitalize = true;
            } else {
                result.append(capitalize
                        ? Character.toUpperCase(character)
                        : character);
                capitalize = false;
            }
        }
        return result.toString();
    }

    private String money(BigDecimal amount) {
        return AMOUNT_FORMAT.format(
                amount == null ? BigDecimal.ZERO : amount);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "-";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(1, maxLength - 3)) + "...";
    }

    private String pdfSafe(String value) {
        String cleaned = safe(value);
        StringBuilder result = new StringBuilder();

        for (char character : cleaned.toCharArray()) {
            result.append(character >= 32 && character <= 126
                    ? character : ' ');
        }
        return result.toString();
    }

    private String safe(String value) {
        return value == null || value.isBlank()
                ? "-"
                : value.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
