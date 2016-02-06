package de.incentergy.letter.sender.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.ejb.Stateless;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.AcroFields.FieldPosition;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfFormField;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.TextField;
import com.itextpdf.text.pdf.codec.PngImage;

/**
 * http://itextpdf.com/examples/itext-action-second-edition/chapter-8
 * 
 * @author manuel
 *
 */
@Stateless
public class PdfAcroFormFiller {

	// http://developers.itextpdf.com/examples/itext-action-second-edition/chapter-15#582-svglayers.java

	/*
	 * // The SVG document factory. protected SAXSVGDocumentFactory factory; //
	 * The SVG bridge context. protected BridgeContext ctx; // The GVT builder
	 * protected GVTBuilder builder;
	 * 
	 * @PostConstruct public void init() { String parser =
	 * XMLResourceDescriptor.getXMLParserClassName(); factory = new
	 * SAXSVGDocumentFactory(parser);
	 * 
	 * UserAgent userAgent = new UserAgentAdapter(); DocumentLoader loader = new
	 * DocumentLoader(userAgent); ctx = new BridgeContext(userAgent, loader);
	 * ctx.setDynamicState(BridgeContext.DYNAMIC);
	 * 
	 * builder = new GVTBuilder(); }
	 */

	public byte[] fillForm(String filePath, Map<String, String> values)
			throws IOException, DocumentException {
		return fillForm(filePath, values, null);
	}

	public byte[] fillForm(String filePath, Map<String, String> values,
			Consumer<Object> callback) throws IOException, DocumentException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(
				1024 * 1024);
		PdfReader reader = new PdfReader(filePath);
		PdfStamper stamper = new PdfStamper(reader, byteArrayOutputStream);
		final AcroFields form = stamper.getAcroFields();
		for (Entry<String, String> e : values.entrySet()) {
			if (e.getKey().equals("signature-drawing.signature-drawing")) {
				for (FieldPosition fp : form.getFieldPositions(
						"signature-drawing.signature-drawing")) {
					PdfContentByte cb = stamper.getOverContent(fp.page);

					cb.saveState();

					InputStream stream = new ByteArrayInputStream(Base64
							.getDecoder().decode(e.getValue().getBytes()));

					Image signatureDrawing = PngImage.getImage(stream);
					signatureDrawing.setAbsolutePosition(fp.position.getLeft(),
							fp.position.getBottom());
					cb.addImage(signatureDrawing);

					/*
					 * Batik had multiple problems; - no support for rgba -
					 * unreliable rendering in an extra thread - no easy build
					 * with maven - ...
					 * 
					 * PdfTemplate signatureTemplate = cb.createTemplate(
					 * fp.position.getWidth(), fp.position.getHeight());
					 * 
					 * final Graphics2D g2d = new
					 * PdfGraphics2D(signatureTemplate, fp.position.getWidth(),
					 * fp.position.getHeight());
					 * 
					 * SVGDocument signature = factory.createSVGDocument(
					 * "signature.svg", new
					 * ByteArrayInputStream(e.getValue().getBytes())); if
					 * (callback != null) { callback.accept(signature); }
					 * GraphicsNode signatureGraphics = builder.build(ctx,
					 * signature);
					 * 
					 * signatureGraphics.paint(g2d);
					 * 
					 * g2d.dispose();
					 * 
					 * cb.addTemplate(signatureTemplate, fp.position.getLeft(),
					 * fp.position.getTop());
					 */
					cb.restoreState();
				}
			} else {
				form.setField(e.getKey(), e.getValue());
			}
		}
		stamper.close();
		reader.close();
		return byteArrayOutputStream.toByteArray();
	}

	public byte[] addReceiverToFormAsAuthor(Object pdfReaderObject,
			Object receiverForLetter) throws DocumentException, IOException {
		PdfReader reader = (PdfReader) pdfReaderObject;
		String receiver = (String) receiverForLetter;
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(
				1024 * 1024);
		PdfStamper stamper = new PdfStamper(reader, byteArrayOutputStream);
		Map<String, String> info = new HashMap<>();
		info.put("Author", receiver);
		stamper.setMoreInfo(info);
		stamper.close();
		reader.close();
		return byteArrayOutputStream.toByteArray();
	}

	public byte[] addSignatureField(byte[] pdfWithReceiver, int page, float llx,
			float lly, float urx, float ury)
					throws IOException, DocumentException {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfReader reader = new PdfReader(
				new ByteArrayInputStream(pdfWithReceiver));
		PdfStamper stamp = new PdfStamper(reader, out);

		PdfWriter writer = stamp.getWriter();
		PdfFormField signatureDrawing = PdfFormField.createEmpty(writer);
		signatureDrawing.setFieldName("signature-drawing");
		/*
		 * @param llx lower left x
		 * 
		 * @param lly lower left y
		 * 
		 * @param urx upper right x
		 * 
		 * @param ury upper right y
		 */
		TextField field = new TextField(writer,
				new Rectangle(llx, lly, urx, ury), "signature-drawing");

		signatureDrawing.addKid(field.getTextField());

		// add the field here, the second param is the page you want it on
		stamp.addAnnotation(signatureDrawing, page);

		stamp.close();
		reader.close();
		return out.toByteArray();
	}

	public byte[] addFirstPageWithAddress(byte[] pdfWithReceiver,
			String receiverAddress) throws IOException, DocumentException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfReader reader = new PdfReader(
				new ByteArrayInputStream(pdfWithReceiver));
		PdfStamper stamper = new PdfStamper(reader, out);
		stamper.insertPage(1, reader.getPageSizeWithRotation(1));

		PdfContentByte cb = stamper.getOverContent(1);

		// Points. 1/72 of an inch. This is the standard PDF unit of measurement
		// Use DIN 5008 Anschrift
		// https://de.wikipedia.org/wiki/DIN_5008#/media/File:DIN_5008,_Form_A.svg
		// 1 inch = 25,4 mm
		// 1 point = 25,4mm/72 = 0.35277777777mm

		cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA,
				BaseFont.WINANSI, false), 12);

		float x = (float) ((20 + 5) / 0.352777);
		float y = (float) ((297 - (27 + 5 + 12.7 + 27.3)) / 0.3527777);

		ColumnText ct = new ColumnText(cb);
		Phrase myText = new Phrase(receiverAddress);
		ct.setSimpleColumn(myText, x, y, (float) (x + (80f / 0.3527777)),
				(float) (y - (27f / 0.3527777)), 15f, Element.ALIGN_LEFT);
		ct.go();
		stamper.close();
		reader.close();
		return out.toByteArray();

	}
}
