package eu.freme.bservices.internationalization.okapi.nif.filter;

import com.hp.hpl.jena.rdf.model.Model;
import eu.freme.bservices.internationalization.okapi.nif.step.NifParameters;
import eu.freme.common.conversion.rdf.JenaRDFConversionService;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.skeleton.ISkeletonWriter;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

/**
 * Writer filter class for NIF documents. It handles filter events from Okapi
 * pipeline and creates a NIF file.
 */
public abstract class AbstractNifWriterFilter implements IFilterWriter {

	/** The string preceding the offset in the URI string. (NIF 2.0) */
	protected final static String URI_CHAR_OFFSET = "#char=";
	
	/** The string preceding the offset in the URI string. (NIF 2.1) */
	protected final static String URI_OFFSET = "#offset_";

	/**
	 * the default URI prefix for NIF resources. It is only used if a custom URI
	 * is not specified.
	 */
	protected final static String DEF_URI_PREFIX = "http://example.org/";

	/** The path where the output file is saved. */
	protected String fwOutputPath;

	/** The output stream. */
	protected OutputStream fwOutputStream;

	/** The parameters set for this file. */
	protected NifParameters params;

	/** The original document name. */
	protected String originalDocName;

	/** The Jena model used for managing the NIF file. */
	protected Model model;

	/** The URI prefix for NIF resources. */
	protected String uriPrefix;

	/** The source locale. */
	protected LocaleId sourceLocale;

	private Logger logger = Logger.getLogger(AbstractNifWriterFilter.class);

	/**
	 * Constructor.
	 * @param params the filter parameters.
	 * @param sourceLocale the source locale.
	 */
	public AbstractNifWriterFilter(NifParameters params, LocaleId sourceLocale) {
		this.params = params;
		this.sourceLocale = sourceLocale;
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.okapi.common.filterwriter.IFilterWriter#getName()
	 */
	@Override
	public String getName() {
		return getClass().getName();
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.okapi.common.filterwriter.IFilterWriter#setOptions(net.sf.okapi.common.LocaleId, java.lang.String)
	 */
	@Override
	public void setOptions(LocaleId locale, String defaultEncoding) {
		// do nothing

	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.okapi.common.filterwriter.IFilterWriter#setOutput(java.lang.String)
	 */
	@Override
	public void setOutput(String path) {
		this.fwOutputPath = path;

	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.okapi.common.filterwriter.IFilterWriter#setOutput(java.io.OutputStream)
	 */
	@Override
	public void setOutput(OutputStream output) {
		this.fwOutputStream = output;
	}

	/**
	 * Write the model into a file.
	 * 
	 * @see IFilterWriter#close()
	 */
	@Override
	public void close() {
		OutputStream outputStream = null;
		try {

			String outputPath = params.getOutputURI();
			if (outputPath == null || outputPath.isEmpty()) {
				String outDirPath = params.getOutBasePath();
				if (outDirPath == null || outDirPath.isEmpty()) {
					if (fwOutputPath != null) {
						outputPath = fwOutputPath;
					}
				} else {
					outputPath = new File(outDirPath, originalDocName
							+ getOutFileExtension()).toURI().toString();
				}
			}

			if (outputPath != null) {
				Util.createDirectories(outputPath);
				File file = new File(new URI(outputPath));
				if (file.exists()) {
					boolean fileDeleted = file.delete();
					logger.debug("fileDeleted: "+fileDeleted);
				}
				logger.debug("outputPath: "+outputPath);
				//System.out.println(outputPath);
				file.createNewFile();
				outputStream = new FileOutputStream(file);

			} else {
				outputStream = fwOutputStream;
			}

			if (outputStream != null) {
				final String nifLang = params.getNifLanguage();
				OutputStreamWriter writer = new OutputStreamWriter(
						outputStream, Charset.forName("UTF-8").newEncoder());
				if (nifLang != null && !nifLang.isEmpty()) {
					logger.debug("nifLang: "+nifLang);
					//System.out.println(nifLang);
					model.write(writer, nifLang);
				} else {
					model.write(writer);
				}
				outputStream.close();
				/*
				// dummy deletion to detect file access
				if (file!=null && file.exists()) {
					boolean fileDeleted = file.delete();
					logger.debug("fileDeleted: "+fileDeleted);
				}*/
			}

		} catch (IOException e) {
			throw new OkapiIOException("Error while saving the NIF file.", e);
		} catch (URISyntaxException e) {
			throw new OkapiIOException("invalid output file URI syntax.", e);
		}

	}

	/**
	 * Gets the appropriate output file extension based on the RDF serialization
	 * format chosen for the NIF file.
	 * 
	 * @return the file extension
	 */
	private String getOutFileExtension() {
		String nifLanguage = params.getNifLanguage();
		String ext = ".rdf";
		if (nifLanguage != null) {
			if (nifLanguage.equals(JenaRDFConversionService.JENA_TURTLE)) {
				ext = ".ttl";
			} else if (nifLanguage.equals(JenaRDFConversionService.JENA_JSON_LD)) {
				ext = ".json";
			}
		}
		return ext;
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.okapi.common.filterwriter.IFilterWriter#getParameters()
	 */
	@Override
	public IParameters getParameters() {
		return params;
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.okapi.common.filterwriter.IFilterWriter#setParameters(net.sf.okapi.common.IParameters)
	 */
	@Override
	public void setParameters(IParameters params) {
		if (!(params instanceof NifParameters)) {
			throw new IllegalArgumentException("Received params of type "
					+ params.getClass().getName()
					+ ". Only NifParameters accepted.");
		}
		this.params = (NifParameters) params;

	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.okapi.common.filterwriter.IFilterWriter#cancel()
	 */
	@Override
	public void cancel() {
		// do nothing

	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.okapi.common.filterwriter.IFilterWriter#getEncoderManager()
	 */
	@Override
	public EncoderManager getEncoderManager() {
		// do nothing
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.okapi.common.filterwriter.IFilterWriter#getSkeletonWriter()
	 */
	@Override
	public ISkeletonWriter getSkeletonWriter() {
		// do nothing
		return null;
	}

}
