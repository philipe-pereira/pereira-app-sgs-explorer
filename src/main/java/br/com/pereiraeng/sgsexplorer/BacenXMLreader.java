package br.com.pereiraeng.sgsexplorer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import br.com.pereiraeng.core.TimeUtils;
import br.com.pereiraeng.core.collections.ArrayUtils;
import br.com.pereiraeng.io.soap.SOAP;
import br.com.pereiraeng.io.soap.vo.VO;
import br.com.pereiraeng.io.soap.vo.VOreader;
import br.com.pereiraeng.math.timeseries.RegP;
import br.com.pereiraeng.math.timeseries.unit.Med;
import br.com.pereiraeng.sgsexplorer.SerieXmlParser.Sample;

public class BacenXMLreader extends VOreader {

	private static final String BACEN = "https://www3.bcb.gov.br/wssgs/services/FachadaWSSGS?wsdl";

	private static final String HEADER = "<soapenv:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:pub=\"http://publico.ws.casosdeuso.sgs.pec.bcb.gov.br\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">\r\n   <soapenv:Header/>\r\n   <soapenv:Body>\r\n",
			H_ST = "      <pub:%s soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\r\n",
			F_ST = "      </pub:%s>\r\n", FOOTER = "   </soapenv:Body>\r\n</soapenv:Envelope>";

	private static final String FUNCTION_1 = "getValoresSeriesXML", FUNCTION_3 = "getUltimosValoresSerieVO",
			FUNCTION_4 = "getValor", FUNCTION_2 = "getUltimoValorXML";

	private final int[] indices;

	public BacenXMLreader(int... indices) {
		this.indices = indices;
	}

	public void read(Calendar[] period) {
		this.function = 1;
		this.med = new RegP(this.indices.length, 1440);

		// ----------------------------------------------------

		StringBuilder sb = new StringBuilder(HEADER);
		sb.append(String.format(H_ST, FUNCTION_1));

		sb.append("         <in0 xsi:type=\"def:ArrayOfflong\" soapenc:arrayType=\"xsd:long[");
		sb.append(indices.length);
		sb.append("]\" xmlns:def=\"http://DefaultNamespace\">\r\n");
		for (int i = 0; i < indices.length; i++) {
			sb.append("            <item xsi:type=\"xsd:long\">");
			sb.append(indices[i]);
			sb.append("</item>\r\n");
		}
		sb.append("         </in0>\r\n");

		sb.append(String.format("         <in1 xsi:type=\"xsd:string\">%1$td/%1$tm/%1$tY</in1>\r\n", period[0]));
		sb.append(String.format("         <in2 xsi:type=\"xsd:string\">%1$td/%1$tm/%1$tY</in2>\r\n", period[1]));

		sb.append(String.format(F_ST, FUNCTION_1));
		sb.append(FOOTER);

		try {
			InputStream is = SOAP.getSOAP(BACEN, sb.toString().getBytes(), "text/xml;charset=UTF-8", "");
			super.parse(new InputSource(is));
			is.close();
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public void read() {
		this.function = 2;
		this.meds = new Med[this.indices.length];

		// ----------------------------------------------------

		for (index = 0; index < this.indices.length; index++) {
			StringBuilder sb = new StringBuilder(HEADER);
			sb.append(String.format(H_ST, FUNCTION_2));

			sb.append(String.format("         <in0 xsi:type=\"xsd:long\">%d</in0>\r\n", this.indices[index]));

			sb.append(String.format(F_ST, FUNCTION_2));
			sb.append(FOOTER);

			try {
				InputStream is = SOAP.getSOAP(BACEN, sb.toString().getBytes(), "text/xml;charset=UTF-8", "");
				super.parse(new InputSource(is));
				is.close();
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}

	public void read(int dias) {
		this.function = 3;
		super.setqName("getUltimosValoresSerieVOReturn");
		this.med = new RegP(this.indices.length, 1440);

		// ----------------------------------------------------

		for (index = 0; index < this.indices.length; index++) {
			StringBuilder sb = new StringBuilder(HEADER);
			sb.append(String.format(H_ST, FUNCTION_3));

			sb.append(String.format("         <in0 xsi:type=\"xsd:long\">%d</in0>\r\n", this.indices[index]));
			sb.append(String.format("         <in1 xsi:type=\"xsd:long\">%d</in1>\r\n", dias));

			sb.append(String.format(F_ST, FUNCTION_3));
			sb.append(FOOTER);

			try {
				InputStream is = SOAP.getSOAP(BACEN, sb.toString().getBytes(), "text/xml;charset=UTF-8", "");
				super.parse(new InputSource(is));
				is.close();
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}

	public void read(Calendar c) {
		this.function = 4;
		this.meds = new Med[this.indices.length];
		this.ci = TimeUtils.toInt(c);

		// ----------------------------------------------------

		for (index = 0; index < this.indices.length; index++) {
			StringBuilder sb = new StringBuilder(HEADER);
			sb.append(String.format(H_ST, FUNCTION_4));

			sb.append(String.format("         <in0 xsi:type=\"xsd:long\">%d</in0>\r\n", this.indices[index]));
			sb.append(String.format(
					"         <in1 xsi:type=\"soapenc:string\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">%1$td/%1$tm/%1$tY</in1>\r\n",
					c));

			sb.append(String.format(F_ST, FUNCTION_4));
			sb.append(FOOTER);

			try {
				InputStream is = SOAP.getSOAP(BACEN, sb.toString().getBytes(), "text/xml;charset=UTF-8", "");
				super.parse(new InputSource(is));
				is.close();
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}

	// ============================ GETTER'S ============================

	private RegP med;

	private Med[] meds;

	public RegP get1() {
		return med;
	}

	public Med[] get2() {
		return meds;
	}

	// ============================ LEITURA DO XML ============================

	private transient int function;

	private transient int index, ci;

	private transient boolean OLD_VERSION = false;

	@Override
	public void characters(String s) {
		if (OLD_VERSION) {
			if (function < 3) { // XML
				String[] rows = s.split("\n");
				if (function == 1) { // dado período
					for (int i = 0; i < rows.length; i++) {
						String r = rows[i].trim();
						if (r.startsWith("<SERIE ID="))
							index = ArrayUtils.indexOf(indices, Integer.parseInt(r.substring(11, r.length() - 2)));
						else if (r.startsWith("<DATA>"))
							ci = TimeUtils.toInt(TimeUtils.string2Date(r.substring(6, r.length() - 7), "dd/MM/yyyy"));
						else if (r.startsWith("<VALOR>")) {
							int e = r.length() - 8;
							if (e > 7)
								med.put(ci, index, Float.parseFloat(r.substring(7, e)));
						}
					}
				} else { // último dia
					ci = 0;
					for (int i = 0; i < rows.length; i++) {
						String r = rows[i].trim();
						if (r.startsWith("<DIA>"))
							ci = Integer.parseInt(r.substring(5, r.length() - 6));
						else if (r.startsWith("<MES>"))
							ci += 100 * (Integer.parseInt(r.substring(5, r.length() - 6)) - 1);
						else if (r.startsWith("<ANO>"))
							ci = TimeUtils.toInt(new GregorianCalendar(Integer.parseInt(r.substring(5, r.length() - 6)),
									ci / 100, ci % 100));
						else if (r.startsWith("<VALOR>")) {
							int e = r.length() - 8;
							if (e > 7)
								meds[index] = new Med(TimeUtils.toCalendar(ci),
										Float.parseFloat(r.substring(7, e).replace(".", "").replace(',', '.')));
						}
					}
				}
			} else {
				if (function == 4) {
					try {
						meds[index] = new Med(TimeUtils.toCalendar(ci), Float.parseFloat(s));
					} catch (NumberFormatException e) {
					}
				} else
					super.characters(s);
			}
		} else {
			try {
				NavigableMap<Integer, Sample> values = new TreeMap<Integer, Sample>();
				int serie = SerieXmlParser.parseToTimeSeries(values, s);
				index = ArrayUtils.indexOf(indices, serie);
				for (Entry<Integer, Sample> e : values.entrySet()) {
					med.put(e.getKey(), index, (float) e.getValue().getValor());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	@Override
	public void endDocument() throws SAXException {
		if (function == 3) {
			VO values = super.getVo().get("valores");
			for (Iterator<VO> iter = values.iterator(); iter.hasNext();) {
				VO vo = iter.next();
				this.med.put(
						new GregorianCalendar(Integer.parseInt(vo.get("ano").get()),
								Integer.parseInt(vo.get("mes").get()), Integer.parseInt(vo.get("dia").get())),
						index, Float.parseFloat(vo.get("valor").get()));
			}
		}
	}
}