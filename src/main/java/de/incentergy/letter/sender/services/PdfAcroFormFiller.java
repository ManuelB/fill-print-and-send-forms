package de.incentergy.letter.sender.services;

import java.awt.Graphics2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.ImgTemplate;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.AcroFields.FieldPosition;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfFormField;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.TextField;

/**
 * http://itextpdf.com/examples/itext-action-second-edition/chapter-8
 * 
 * @author manuel
 *
 */
@Stateless
public class PdfAcroFormFiller {

	// http://developers.itextpdf.com/examples/itext-action-second-edition/chapter-15#582-svglayers.java

	/** The SVG document factory. */
	protected SAXSVGDocumentFactory factory;
	/** The SVG bridge context. */
	protected BridgeContext ctx;
	/** The GVT builder */
	protected GVTBuilder builder;

	@PostConstruct
	public void init() {
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		factory = new SAXSVGDocumentFactory(parser);

		UserAgent userAgent = new UserAgentAdapter();
		DocumentLoader loader = new DocumentLoader(userAgent);
		ctx = new BridgeContext(userAgent, loader);
		ctx.setDynamicState(BridgeContext.DYNAMIC);

		builder = new GVTBuilder();
	}

	public byte[] fillForm(String filePath, Map<String, String> values)
			throws IOException, DocumentException {
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

					PdfTemplate signatureTemplate = cb.createTemplate(
							fp.position.getWidth(), fp.position.getHeight());

					Graphics2D g2d = new PdfGraphics2D(signatureTemplate,
							fp.position.getWidth(), fp.position.getHeight());
					SVGDocument signature = factory.createSVGDocument(
							"signature.svg",
							new ByteArrayInputStream(e.getValue().getBytes()));
					GraphicsNode signatureGraphics = builder.build(ctx,
							signature);
					signatureGraphics.paint(g2d);
					g2d.dispose();
					cb.addTemplate(signatureTemplate, fp.position.getLeft(),
							fp.position.getTop());
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
}
