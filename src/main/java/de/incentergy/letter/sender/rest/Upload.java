package de.incentergy.letter.sender.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.itextpdf.text.DocumentException;

import de.incentergy.letter.sender.services.PdfAcroFormFiller;

@Stateless
@Path("/upload")
public class Upload {

	private static final Logger log = Logger.getLogger(Upload.class.getName());

	@Inject
	PdfAcroFormFiller pdfAcroFormFiller;

	@Inject
	PropertiesConfiguration config;

	/**
	 * https://docs.jboss.org/resteasy/docs/1.1.GA/userguide/html/Multipart.html
	 * 
	 * @param deliveries
	 * @param file
	 * @param context
	 * @return
	 * @throws IOException
	 */
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response post(Map<String, Object> formData,
			@Context ServletContext context) {

		String pdfFileName = (String) formData.get("pdf-file-name");
		byte[] pdfWithReceiver;
		try {
			pdfWithReceiver = pdfAcroFormFiller.addReceiverToFormAsAuthor(
					formData.get("file"), formData.get("receiver-for-letter"));
			
			try {
				pdfWithReceiver = pdfAcroFormFiller.addSignatureField(pdfWithReceiver,
					Integer.parseInt((String) formData.get("page")),
					Float.parseFloat((String) formData.get("signature-llx")),
					Float.parseFloat((String) formData.get("signature-lly")),
					Float.parseFloat((String) formData.get("signature-urx")),
					Float.parseFloat((String) formData.get("signature-ury")));
			} catch(Exception ex) {
				log.log(Level.WARNING, "Cannot add Signature coordinate", ex);
			}

			String realPath = config.getString("uploadPath",
					context.getRealPath("/documents/")) + "/" + pdfFileName;

			File file2 = new File(realPath);
			int i = 2;
			while (file2.exists()) {
				if(i == 2) {
					pdfFileName = pdfFileName.replace(".pdf", "-" + i + ".pdf");
				} else {
					pdfFileName = pdfFileName.replace("-" + (i-1) + ".pdf", "-" + i + ".pdf");
				}
				realPath = config.getString("uploadPath",
						context.getRealPath("/documents/")) + "/" + pdfFileName;

				file2 = new File(realPath);
				// throw new IllegalStateException("File does already exist.");
				i++;
			}

			try (FileOutputStream fo = new FileOutputStream(file2)) {
				fo.write(pdfWithReceiver);
				fo.close();
			}
		} catch (DocumentException | IOException e) {
			return Response.serverError().entity(e).build();
		}

		return Response.ok().entity(pdfFileName).build();
	}
}