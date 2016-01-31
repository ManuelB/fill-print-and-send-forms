package de.incentergy.letter.sender.services;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.itextpdf.text.DocumentException;

public class PdfAcroFormFillerTest {

	@Test
	public void testFillForm() throws IOException, DocumentException {
		PdfAcroFormFiller pdfAcroFormFiller = new PdfAcroFormFiller();
		pdfAcroFormFiller.init();
		String filePath = "src/main/webapp/upload/05_AAG_AU.pdf";

		Map<String, String> values = new HashMap<String, String>();
		values.put("Betriebsnummer", "50661453");
		values.put("Name", "Manuel Blechschmidt");
		values.put("Straße", "Schenkendorfstr.");
		values.put("Nr", "3");
		values.put("PLZ", "56068");
		values.put("Ort", "Koblenz");
		// The typo is correct, don't fix this
		values.put("Ansprechpatner", "Manuel Blechschmidt");
		values.put("Telefon", "+491736322621");
		values.put("Telefax", "+4926128759322");
		values.put("E-Mail", "manuel.blechschmidt@gmx.de");
		values.put("Name_01", "Driesch-Etscheid");
		values.put("Vorname", "Elke");
		values.put("signature-drawing.signature-drawing",
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\"><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"261\" height=\"60\"><path stroke-linejoin=\"round\" stroke-linecap=\"round\" stroke-width=\"2\" stroke=\"rgba(0, 0, 0, 0.870588)\" fill=\"none\" d=\"M 1 23 c 0.03 -0.07 1.01 -3.16 2 -4 c 2.88 -2.44 8.5 -6.29 11 -7 c 0.81 -0.23 2.64 1.85 3 3 c 1.39 4.4 2.24 10.69 3 16 c 0.23 1.63 -0.47 3.89 0 5 c 0.34 0.8 2.59 2.33 3 2 c 0.72 -0.57 1.05 -4.2 2 -6 c 1.97 -3.72 6.12 -11.49 7 -11 c 1.09 0.6 1.01 11.29 2 16 c 0.22 1.05 1.2 2.66 2 3 c 1.11 0.47 3.8 0.53 5 0 c 1.36 -0.6 2.9 -2.51 4 -4 c 3.51 -4.76 5.7 -11.93 10 -15 c 10.46 -7.47 33.98 -19.47 39 -20 c 1.84 -0.19 -0.27 11.16 -1 16 c -0.2 1.35 -1.71 2.7 -2 4 c -0.32 1.46 0 5 0 5 c 0 0 0.47 -3.89 0 -5 c -0.34 -0.8 -2.54 -2.41 -3 -2 c -1.48 1.31 -5.47 7.7 -6 10 c -0.19 0.84 1.9 2.86 3 3 c 5.21 0.68 13.95 0.94 20 0 c 3.93 -0.61 9.18 -4.62 12 -5 c 0.92 -0.12 2.85 1.9 3 3 c 0.67 4.94 -0.95 17.93 0 19 c 0.64 0.72 5.01 -7.16 8 -10 c 3.5 -3.33 8.53 -5.72 12 -9 c 2.31 -2.19 6.88 -7.46 6 -8 c -1.67 -1.03 -18.04 0.22 -19 0 c -0.34 -0.08 3.94 -2.54 6 -3 c 6.51 -1.45 13.93 -2.53 21 -3 c 8 -0.53 23.58 -0.03 24 0 c 0.18 0.01 -10 0.67 -10 1 c 0 0.34 6.67 1.71 10 2 c 4.25 0.37 8.78 0.53 13 0 c 6.3 -0.79 12.53 -2.88 19 -4 c 3.35 -0.58 6.88 -0.09 10 -1 c 10.2 -2.99 20.66 -7.65 31 -11 c 1.91 -0.62 4.25 -0.42 6 -1 c 1.04 -0.35 3.06 -2.09 3 -2 c -0.17 0.26 -5 5.08 -7 8 c -7.35 10.76 -13.85 21.57 -21 33 c -1.47 2.36 -3 4.58 -4 7 c -1.29 3.13 -3 9.82 -3 10 c 0 0.15 1.69 -5.93 3 -8 c 0.77 -1.21 3.17 -1.72 4 -3 c 5.95 -9.19 19.51 -27.11 18 -31 c -1.26 -3.26 -20.23 -0.8 -30 0 c -6.33 0.52 -12.73 3 -19 4 c -1.94 0.31 -6.07 -0.07 -6 0 c 0.09 0.09 4.64 0.91 7 1 c 6.34 0.24 17.37 -0.31 19 0 c 0.36 0.07 -1.87 2.43 -3 3 l -9 3\"/></svg>");
		values.put("Does-not-exists", "Text");
		byte[] document = pdfAcroFormFiller.fillForm(filePath, values);
		FileOutputStream fo = new FileOutputStream(
				"target/05_AAG_AU.filled.pdf");
		fo.write(document);
		fo.close();
	}

	@Test
	public void testAddSignatureField() throws IOException, DocumentException {
		PdfAcroFormFiller pdfAcroFormFiller = new PdfAcroFormFiller();
		String filePath = "src/main/webapp/upload/05_AAG_AU.pdf";
		Path path = Paths.get(filePath);
		byte[] data = Files.readAllBytes(path);

		// llx 122.66666666666667
		// lly 48.55666666666669
		// urx 328
		// ury 83.22333333333336

		int pageNumber = 1;
		float llx = 122.66666f;
		float lly = 48.5566666f;
		float urx = 328f;
		float ury = 83.223333f;
		byte[] document = pdfAcroFormFiller.addSignatureField(data, pageNumber,
				llx, lly, urx, ury);
		FileOutputStream fo = new FileOutputStream(
				"target/05_AAG_AU.with-signature.pdf");
		fo.write(document);
		fo.close();
	}

}
