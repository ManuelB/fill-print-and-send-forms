package de.incentergy.letter.sender.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

/**
 * The File servlet for serving from absolute path.
 * 
 * @author BalusC
 * @link http://balusc.blogspot.com/2007/07/fileservlet.html
 */
@WebServlet(urlPatterns = { "/documents/*" })
public class FileServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -929847426318252860L;

	private static final Logger log = Logger
			.getLogger(FileServlet.class.getName());

	// Constants
	// ----------------------------------------------------------------------------------

	private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.

	// Properties
	// ---------------------------------------------------------------------------------

	private PropertiesConfiguration config;

	// Actions
	// ------------------------------------------------------------------------------------

	public void init() throws ServletException {

		// Define base path somehow. You can define it as init-param of the
		// servlet.
		try {
			config = new PropertiesConfiguration(Thread.currentThread()
					.getContextClassLoader().getResource("config.properties"));
			config.setReloadingStrategy(new FileChangedReloadingStrategy());
		} catch (ConfigurationException e) {
			log.log(Level.WARNING, "Could not load decide-it.properties", e);
		}

		// In a Windows environment with the Applicationserver running on the
		// c: volume, the above path is exactly the same as "c:\files".
		// In UNIX, it is just straightforward "/files".
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// Get requested file by path info.
		String requestedFile = request.getPathInfo();

		// Check if file is actually supplied to the request URI.
		if (requestedFile == null) {
			// Do your thing if the file is not supplied to the request URI.
			// Throw an exception, or send 404, or show default/warning page, or
			// just ignore it.
			response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
			return;
		}

		// Decode the file name (might contain spaces and on) and prepare file
		// object.
		File file = new File(
				config.getString("uploadPath",
						getServletContext().getRealPath("/upload/")),
				URLDecoder.decode(requestedFile, "UTF-8"));

		// Check if file actually exists in filesystem.
		if (!file.exists()) {
			// Do your thing if the file appears to be non-existing.
			// Throw an exception, or send 404, or show default/warning page, or
			// just ignore it.
			response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
			return;
		}

		// Get content type by filename.
		String contentType = getServletContext().getMimeType(file.getName());

		// If content type is unknown, then set the default value.
		// For all content types, see:
		// http://www.w3schools.com/media/media_mimeref.asp
		// To add new content types, add new mime-mapping entry in web.xml.
		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		// Init servlet response.
		response.reset();
		response.setBufferSize(DEFAULT_BUFFER_SIZE);
		response.setContentType(contentType);
		response.setHeader("Content-Length", String.valueOf(file.length()));
		//response.setHeader("Content-Disposition",
		//		"attachment; filename=\"" + file.getName() + "\"");

		// Prepare streams.
		BufferedInputStream input = null;
		BufferedOutputStream output = null;

		try {
			// Open streams.
			input = new BufferedInputStream(new FileInputStream(file),
					DEFAULT_BUFFER_SIZE);
			output = new BufferedOutputStream(response.getOutputStream(),
					DEFAULT_BUFFER_SIZE);

			// Write file contents to response.
			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			int length;
			while ((length = input.read(buffer)) > 0) {
				output.write(buffer, 0, length);
			}
		} finally {
			// Gently close streams.
			close(output);
			close(input);
		}
	}

	// Helpers (can be refactored to public utility class)
	// ----------------------------------------

	private static void close(Closeable resource) {
		if (resource != null) {
			try {
				resource.close();
			} catch (IOException e) {
				// Do your thing with the exception. Print it, log it or mail
				// it.
				log.log(Level.WARNING, "Problem", e);
			}
		}
	}

}