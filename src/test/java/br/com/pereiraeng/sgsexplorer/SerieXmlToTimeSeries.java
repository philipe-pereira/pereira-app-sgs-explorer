package br.com.pereiraeng.sgsexplorer;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

public class SerieXmlToTimeSeries {

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d/M/uuuu");

	/**
	 * Parseia TODOS os <ITEM> (independente de SERIE ID). Se quiser filtrar por ID
	 * depois, eu ajusto.
	 */
	public static TimeSeriesTable parse(String xml) throws Exception {
		if (xml == null)
			throw new IllegalArgumentException("xml == null");

		TimeSeriesTable ts = new TimeSeriesTable(256);

		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

		XMLStreamReader r = null;

		String currentElement = null;
		boolean insideItem = false;

		LocalDate data = null;
		Double valor = null;
		Boolean bloqueado = null;

		try {
			r = factory.createXMLStreamReader(new StringReader(xml));

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

						int t = (int) data.toEpochDay();
						ts.put(t, valor.doubleValue(), bloqueado.booleanValue());
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

		ts.compact();
		return ts;
	}

	// demo
	public static void main(String[] args) throws Exception {
		String xml = "<?xml version='1.0' encoding='ISO-8859-1'?>\n" + "<SERIES>\n" + "  <SERIE ID='11'>\n"
				+ "    <ITEM><DATA>20/1/2026</DATA><VALOR>0.055131</VALOR><BLOQUEADO>false</BLOQUEADO></ITEM>\n"
				+ "    <ITEM><DATA>23/1/2026</DATA><VALOR>0.123</VALOR><BLOQUEADO>true</BLOQUEADO></ITEM>\n"
				+ "  </SERIE>\n" + "</SERIES>";

		TimeSeriesTable ts = parse(xml);
		System.out.println("size=" + ts.size());

		TimeSeriesTable.IntRange r = ts.rangeInclusive((int) LocalDate.of(2026, 1, 20).toEpochDay(),
				(int) LocalDate.of(2026, 1, 30).toEpochDay());
		System.out.println("range=" + r);

		for (int i = r.from; i < r.toExclusive; i++) {
			System.out.println(ts.timeAt(i) + " -> " + ts.valorAt(i) + " bloqueado=" + ts.bloqueadoAt(i));
		}
	}
}
