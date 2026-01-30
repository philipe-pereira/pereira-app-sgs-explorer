package br.com.pereiraeng.sgsexplorer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import br.com.pereiraeng.core.TimeUtils;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.NavigableMap;

public final class SerieXmlParser {

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d/M/uuuu");

	public static final class Sample {
		private final double valor;
		private final boolean bloqueado;

		public Sample(double valor, boolean bloqueado) {
			this.valor = valor;
			this.bloqueado = bloqueado;
		}

		public double getValor() {
			return valor;
		}

		public boolean isBloqueado() {
			return bloqueado;
		}

		@Override
		public String toString() {
			return "Sample{valor=" + valor + ", bloqueado=" + bloqueado + "}";
		}
	}

	public static int parseToTimeSeries(NavigableMap<Integer, Sample> series, String xml) throws Exception {
		if (xml == null)
			throw new IllegalArgumentException("xml == null");

		XMLInputFactory factory = XMLInputFactory.newFactory();
		// Segurança/robustez básica
		factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

		XMLStreamReader r = null;
		try {
			r = factory.createXMLStreamReader(new StringReader(xml));

			String currentElement = null;

			boolean insideItem = false;

			LocalDate data = null;
			Double valor = null;
			Boolean bloqueado = null;

			while (r.hasNext()) {
				int event = r.next();

				switch (event) {
				case XMLStreamConstants.START_ELEMENT:
					currentElement = r.getLocalName();
					if ("ITEM".equals(currentElement)) {
						insideItem = true;
						data = null;
						valor = null;
						bloqueado = null;
					} else if ("SERIE".equals(currentElement)) {
						// TODO
					}
					break;

				case XMLStreamConstants.CHARACTERS:
					if (!insideItem)
						break;
					if (currentElement == null)
						break;

					String text = r.getText();
					if (text == null)
						break;

					text = text.trim();
					if (text.length() == 0)
						break;

					if ("DATA".equals(currentElement)) {
						data = LocalDate.parse(text, DATE_FMT);
					} else if ("VALOR".equals(currentElement)) {
						valor = Double.valueOf(Double.parseDouble(text));
					} else if ("BLOQUEADO".equals(currentElement)) {
						bloqueado = Boolean.valueOf(Boolean.parseBoolean(text));
					}
					break;

				case XMLStreamConstants.END_ELEMENT:
					String end = r.getLocalName();

					if ("ITEM".equals(end)) {
						insideItem = false;

						if (data == null || valor == null || bloqueado == null) {
							throw new IllegalArgumentException(
									"ITEM incompleto: DATA=" + data + ", VALOR=" + valor + ", BLOQUEADO=" + bloqueado);
						}

//						int t = (int) data.toEpochDay(); // inteiro tempo (dias)
						int t = TimeUtils.toInt(convertLocalDateToCalendar(data));
						series.put(Integer.valueOf(t), new Sample(valor.doubleValue(), bloqueado.booleanValue()));
					}

					currentElement = null;
					break;

				default:
					break;
				}
			}
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (Exception ignored) {
				}
			}
		}

		return 11;
	}

	public static Calendar convertLocalDateToCalendar(LocalDate localDate) {
		// 1. Add a time component to the LocalDate (start of the day is often desired)
		// Using atStartOfDay() is recommended as it handles potential Daylight Saving
		// Time anomalies
		ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.systemDefault());

		// 2. Convert the ZonedDateTime to a GregorianCalendar using the 'from' method
		GregorianCalendar calendar = GregorianCalendar.from(zonedDateTime);

		return calendar;
	}
}
