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

	// static Consumer<SVGDocument> callback;

	@Test
	public void testFillForm() throws IOException, DocumentException {
		PdfAcroFormFiller pdfAcroFormFiller = new PdfAcroFormFiller();
		// pdfAcroFormFiller.init();
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
		// values.put("signature-drawing.signature-drawing",
		// "<?xml version=\"1.0\" encoding=\"UTF-8\"
		// standalone=\"no\"?><!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"
		// \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\"><svg
		// xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"261\"
		// height=\"60\"><path stroke-linejoin=\"round\"
		// stroke-linecap=\"round\" stroke-width=\"2\" stroke=\"rgba(0, 0, 0,
		// 0.870588)\" fill=\"none\" d=\"M 1 23 c 0.03 -0.07 1.01 -3.16 2 -4 c
		// 2.88 -2.44 8.5 -6.29 11 -7 c 0.81 -0.23 2.64 1.85 3 3 c 1.39 4.4 2.24
		// 10.69 3 16 c 0.23 1.63 -0.47 3.89 0 5 c 0.34 0.8 2.59 2.33 3 2 c 0.72
		// -0.57 1.05 -4.2 2 -6 c 1.97 -3.72 6.12 -11.49 7 -11 c 1.09 0.6 1.01
		// 11.29 2 16 c 0.22 1.05 1.2 2.66 2 3 c 1.11 0.47 3.8 0.53 5 0 c 1.36
		// -0.6 2.9 -2.51 4 -4 c 3.51 -4.76 5.7 -11.93 10 -15 c 10.46 -7.47
		// 33.98 -19.47 39 -20 c 1.84 -0.19 -0.27 11.16 -1 16 c -0.2 1.35 -1.71
		// 2.7 -2 4 c -0.32 1.46 0 5 0 5 c 0 0 0.47 -3.89 0 -5 c -0.34 -0.8
		// -2.54 -2.41 -3 -2 c -1.48 1.31 -5.47 7.7 -6 10 c -0.19 0.84 1.9 2.86
		// 3 3 c 5.21 0.68 13.95 0.94 20 0 c 3.93 -0.61 9.18 -4.62 12 -5 c 0.92
		// -0.12 2.85 1.9 3 3 c 0.67 4.94 -0.95 17.93 0 19 c 0.64 0.72 5.01
		// -7.16 8 -10 c 3.5 -3.33 8.53 -5.72 12 -9 c 2.31 -2.19 6.88 -7.46 6 -8
		// c -1.67 -1.03 -18.04 0.22 -19 0 c -0.34 -0.08 3.94 -2.54 6 -3 c 6.51
		// -1.45 13.93 -2.53 21 -3 c 8 -0.53 23.58 -0.03 24 0 c 0.18 0.01 -10
		// 0.67 -10 1 c 0 0.34 6.67 1.71 10 2 c 4.25 0.37 8.78 0.53 13 0 c 6.3
		// -0.79 12.53 -2.88 19 -4 c 3.35 -0.58 6.88 -0.09 10 -1 c 10.2 -2.99
		// 20.66 -7.65 31 -11 c 1.91 -0.62 4.25 -0.42 6 -1 c 1.04 -0.35 3.06
		// -2.09 3 -2 c -0.17 0.26 -5 5.08 -7 8 c -7.35 10.76 -13.85 21.57 -21
		// 33 c -1.47 2.36 -3 4.58 -4 7 c -1.29 3.13 -3 9.82 -3 10 c 0 0.15 1.69
		// -5.93 3 -8 c 0.77 -1.21 3.17 -1.72 4 -3 c 5.95 -9.19 19.51 -27.11 18
		// -31 c -1.26 -3.26 -20.23 -0.8 -30 0 c -6.33 0.52 -12.73 3 -19 4 c
		// -1.94 0.31 -6.07 -0.07 -6 0 c 0.09 0.09 4.64 0.91 7 1 c 6.34 0.24
		// 17.37 -0.31 19 0 c 0.36 0.07 -1.87 2.43 -3 3 l -9 3\"/></svg>");
		values.put("signature-drawing.signature-drawing",
				"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pxklVZa3FZTz0Dntzj3KzGycSX6Rpv4qCvBjISewtFrDS8ot+/4+G7i8tOCA4JBfzDSXoyz3I1dBRrhFiNP7WBsw0J+MmmYyMa+hVlajFE8HqyOnsj/rOiRgw2S6CMskAyD4JJsg61vpixQOlpWfXYU1vGe8LBURzyG/eIp9nRvn2oCBZ9UVX6UtqBaxqKw6WO1Zbv3eOyWBOcskazVlMuOd6uOlvHfNSYUHlnxNxLy26ruiHawuZUndvHEuWSZLeWA3TyJ3ZkRLJxVEap1/9akth6vxiS1R08cbTQerOzPB/a1dAjdFAvVJBW63qD4AE6XFbfm0Vl1lxL/xU0tR0w5WN2WF9HF0CZyIBOrUFMECQY/h6QfBApwhrkpVCtHt1a1bVqtF1m/oEugSGEggTypkgb/h6YcPKSBVn7ZYLcQOVqtF1m/oEugSGEggTwDIwfrOg04/dDewL7MugS6B85RAt6zOc956r7sELk4CHawubsr7gLsEzlMCHazOc956r7sELk4CHawubsr7gLsEzlMCHazOc956r7sELk4CHawubsr7gLsEzlMCHazOc956r7sELk4CHawubsr7gLsEzlMCHazOc956r7sELk4CHawubsr7gLsEzlMCHazOc956r7sELk4CHawubsr7gLsEzlMCHazOc956r7sELk4CHawubsr7gLsEzlMCHazOc956r7sELk4CVwJWd91110PuvffeR966det+FyfRPuAugQuXwD333POgu++++3FHi+HKwCoiHhURPivdW5dAl8AFSeCswOqC5qUPtUugS+CaJHAlltU19b2/pkugS+CCJNDB6oImuw+1S+CcJdDB6pxnr/e9S+CCJNDB6oImuw+1S+CcJdDB6pxnr/e9S+CCJNDB6oImuw+1S+CcJdDB6pxnr/e9S+CCJNDB6oImuw+1S+CcJdDB6pxnr/e9S+CCJNDB6oImuw+1S+CcJdDB6pxnr/e9S+CCJNDB6oImuw+1S+CcJdDB6pxnr/e9S+CCJPB/1DALtb0KVm4AAAAASUVORK5CYII=");
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

	@Test
	public void testAddFirstPageWithAddress() throws IOException, DocumentException {
		PdfAcroFormFiller pdfAcroFormFiller = new PdfAcroFormFiller();
		// pdfAcroFormFiller.init();
		String filePath = "src/main/webapp/upload/05_AAG_AU.pdf";
		Path path = Paths.get(filePath);
		byte[] data = Files.readAllBytes(path);
		byte[] document = pdfAcroFormFiller.addFirstPageWithAddress(data,
				"Manuel Blechschmidt\nSchenkendorfstr. 3\n56068 Koblenz");
		FileOutputStream fo = new FileOutputStream(
				"target/05_AAG_AU.with-address.pdf");
		fo.write(document);
		fo.close();
	}

	/*
	 * public static void main(String[] args) { PdfAcroFormFillerTest
	 * pdfAcroFormFillerTest = new PdfAcroFormFillerTest(); try {
	 * 
	 * callback = (svgDocument) -> { SwingUtilities.invokeLater(() -> { JFrame
	 * frame = new JFrame("Test Signature");
	 * frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); JSVGCanvas
	 * svgCanvas = new JSVGCanvas(); svgCanvas.setDoubleBufferedRendering(true);
	 * 
	 * svgCanvas.setSVGDocument(svgDocument);
	 * 
	 * final JPanel panel = new JPanel(new BorderLayout());
	 * 
	 * JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT)); JLabel label =
	 * new JLabel(); p.add(label);
	 * 
	 * panel.add("North", p); panel.add("Center", svgCanvas);
	 * 
	 * // Set the JSVGCanvas listeners. svgCanvas.addSVGDocumentLoaderListener(
	 * new SVGDocumentLoaderAdapter() { public void documentLoadingStarted(
	 * SVGDocumentLoaderEvent e) { label.setText("Document Loading..."); }
	 * 
	 * public void documentLoadingCompleted( SVGDocumentLoaderEvent e) {
	 * label.setText("Document Loaded."); } });
	 * 
	 * svgCanvas.addGVTTreeBuilderListener( new GVTTreeBuilderAdapter() { public
	 * void gvtBuildStarted(GVTTreeBuilderEvent e) { label.setText(
	 * "Build Started..."); }
	 * 
	 * public void gvtBuildCompleted(GVTTreeBuilderEvent e) { label.setText(
	 * "Build Done."); frame.pack(); } });
	 * 
	 * svgCanvas.addGVTTreeRendererListener( new GVTTreeRendererAdapter() {
	 * public void gvtRenderingPrepare( GVTTreeRendererEvent e) { label.setText(
	 * "Rendering Started..."); }
	 * 
	 * public void gvtRenderingCompleted( GVTTreeRendererEvent e) {
	 * label.setText(""); } });
	 * 
	 * frame.getContentPane().add(panel); frame.setSize(400, 400);
	 * frame.setVisible(true); }); };
	 * 
	 * pdfAcroFormFillerTest.testFillForm(); } catch (IOException e) {
	 * e.printStackTrace(); } catch (DocumentException e) { e.printStackTrace();
	 * } }
	 */

}
