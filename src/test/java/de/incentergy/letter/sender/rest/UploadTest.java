package de.incentergy.letter.sender.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;

import de.incentergy.letter.sender.services.PdfAcroFormFiller;

public class UploadTest {

	@Test
	public void test() throws DocumentException, IOException {
		PdfAcroFormFiller pdfAcroFormFiller = new PdfAcroFormFiller();
		Pattern p = Pattern.compile(".*? (.*?), (\\d{5}) ((Bad )?.*?) .*");
		Scanner s = new Scanner(new File(
				"/home/manuel/Dropbox/Incentergy/Formulare Internetseite/Formulare für Startphase/Adressen für Kindergeldantrag zur Bearbeitung"));
		List<String> list = new ArrayList<String>();
		while (s.hasNextLine()) {
			String string = s.nextLine();
			Matcher m = p.matcher(string);
			if (m.matches()) {
				String strasse = m.group(1);
				String zip = m.group(2);
				String ort = m.group(3);
				System.out.println(
						"'" + strasse + "' '" + zip + "' '" + ort + "'");

				byte[] pdfWithReceiver = pdfAcroFormFiller
						.addReceiverToFormAsAuthor(
								new PdfReader(new FileInputStream(
										"/var/documents/Final_mit_unterschrift.pdf")),
								"Familenkasse\n" + strasse + "\n" + zip + " "
										+ ort);
				try (FileOutputStream fo = new FileOutputStream(
						"/var/documents/Kindergeld-" + ort + ".pdf")) {
					fo.write(pdfWithReceiver);
					fo.close();
				}
			}
			list.add(string);
			// System.out.println(string);
		}
		s.close();
	}

}
