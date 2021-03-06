package de.incentergy.letter.sender.rest;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;
import com.itextpdf.text.DocumentException;

import de.incentergy.letter.sender.services.PdfAcroFormFiller;

@Stateless
@Path("/send")
public class Sender {

	private static final Logger log = Logger.getLogger(Sender.class.getName());

	@Inject
	PdfAcroFormFiller pdfAcroFormFiller;

	@Resource(mappedName = "java:jboss/mail/Default")
	Session session;

	@Inject
	PropertiesConfiguration config;

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response post(MultivaluedMap<String, String> form,
			@Context ServletContext context) throws MessagingException {

		String formName = form.getFirst("form-file-name");

		String payment_method_nonce = form.getFirst("payment_method_nonce");
		if (payment_method_nonce != null && !payment_method_nonce.equals("")) {
			TransactionRequest request = new TransactionRequest()
					.amount(new BigDecimal("2.99"))
					.paymentMethodNonce(payment_method_nonce);

			Result<Transaction> result = Payment.gateway.transaction()
					.sale(request);
			if (!result.isSuccess()) {
				throw new RuntimeException("Payment Authorization failed.");
			}
		}

		Map<String, String> values = new HashMap<>();
		for (Entry<String, List<String>> entry : form.entrySet()) {
			log.fine(entry.getKey() + " = " + entry.getValue().stream()
					.collect(Collectors.joining(", ")));
			values.put(entry.getKey(), form.getFirst(entry.getKey()));

		}
		String realPath = config.getString("uploadPath",
				context.getRealPath("/documents/")) + "/" + formName;

		try {
			byte[] filledDocument = pdfAcroFormFiller.fillForm(realPath,
					values);

			filledDocument = pdfAcroFormFiller.addFirstPageWithAddress(
					filledDocument, form.getFirst("receiver-for-letter"));

			sendOrderEmail(formName, filledDocument);
			sendConfirmationEmail(form.getFirst("email-for-confirmation"),
					formName, filledDocument);

			return Response.ok().type("application/pdf").entity(filledDocument)
					.build();
		} catch (IOException | DocumentException e) {
			// does not work reliable:
			// Could not find MessageBodyWriter for response object of type:
			// java.io.IOException of media type: text/html
			// return Response.serverError().entity(e).build();
			throw new RuntimeException(e);
		}
	}

	public void sendConfirmationEmail(String receiver, String formName,
			byte[] filledDocument) throws AddressException, MessagingException {
		if (receiver != null && !receiver.equals("")) {
			MimeMessage message = new MimeMessage(session);
			InternetAddress[] address = { new InternetAddress(receiver) };
			message.setRecipients(Message.RecipientType.TO, address);

			message.setFrom(new InternetAddress("service@incentergy.de"));
			message.setSubject(
					"Briefversand über Formularportal der Incentergy GmbH");
			message.setSentDate(new Date());

			// Create a multipart message
			Multipart multipart = new MimeMultipart();
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart
					.setText("Sehr geehrter Kunde,\nSie haben den angehangen "
							+ " Brief heute über unser Portal versendet. Sollten Sie Rückfragen haben,"
							+ " antworten Sie einfach auf diese Email,\n\nMit freundlichen Grüßen\n"
							+ "    Ihr Incentergy Formular Team");
			multipart.addBodyPart(messageBodyPart);

			messageBodyPart = new MimeBodyPart();
			DataSource source = new ByteArrayDataSource(filledDocument,
					"application/pdf");
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(formName);
			multipart.addBodyPart(messageBodyPart);
			message.setContent(multipart);

			Transport.send(message);
		}
	}

	public void sendOrderEmail(String formName, byte[] filledDocument)
			throws AddressException, MessagingException {
		MimeMessage message = new MimeMessage(session);
		InternetAddress[] address = {
				new InternetAddress("order@pixelletter.de"),
				new InternetAddress("manuel.blechschmidt@incentergy.de") };
		message.setRecipients(Message.RecipientType.TO, address);

		message.setFrom(
				new InternetAddress("manuel.blechschmidt@incentergy.de"));
		message.setSubject("Briefversand über Pixelletter");
		message.setSentDate(new Date());

		// Create a multipart message
		Multipart multipart = new MimeMultipart();
		BodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setText("# email: manuel.blechschmidt@incentergy.de\n"
				+ "# pw: Kud8ruvu\n" + "# action: 1\n" + "# agb: ja\n"
				+ "# widerrufsverzicht: ja\n" + "# transaction:\n"
				/*+ "# testmodus: 1\n"*/ + "# location: 1\n" + "# destination: DE");
		multipart.addBodyPart(messageBodyPart);

		messageBodyPart = new MimeBodyPart();
		DataSource source = new ByteArrayDataSource(filledDocument,
				"application/pdf");
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setFileName(formName);
		multipart.addBodyPart(messageBodyPart);
		message.setContent(multipart);

		Transport.send(message);
	}
}
