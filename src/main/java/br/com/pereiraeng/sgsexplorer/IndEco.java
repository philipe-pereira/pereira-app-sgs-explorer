package br.com.pereiraeng.sgsexplorer;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;

import br.com.pereiraeng.core.ColorUtils;
import br.com.pereiraeng.io.IOutils;
import br.com.pereiraeng.math.swing.chart.time.measurement.MeasChart;
import br.com.pereiraeng.math.timeseries.RegP;
import br.com.pereiraeng.math.timeseries.unit.Med;
import br.com.pereiraeng.swing.Grade;
import br.com.pereiraeng.swing.input.time.PeriodInput;
import br.com.pereiraeng.swing.input.time.TimeInput;
import br.com.pereiraeng.swing.list.filter.FilterableList;
import br.com.pereiraeng.swing.table.AdvancedTableModel;

public class IndEco extends JPanel implements ActionListener {
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		JFrame f = new JFrame();

		f.setContentPane(new IndEco());
		f.setSize(800, 600);

		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setVisible(true);
	}

	private JRadioButton pe;
	private JRadioButton p0, p1, p2;
	private PeriodInput pi;
	private TimeInput ti;
	private JSpinner dias;
	private JLabel dl;

	private JPanel med;
	private MeasChart<Integer> chart;
	private AdvancedTableModel atm;

	private FilterableList<String> fl;
	private JList<String> list;
	private DefaultListModel<String> dlm;

	public IndEco() {
		super(new BorderLayout());

		JPanel p = new JPanel();

		ButtonGroup bg = new ButtonGroup();

		pe = new JRadioButton("Período", true);
		pe.addActionListener(this);
		pe.setActionCommand("PE");
		bg.add(pe);
		p.add(pe);

		JRadioButton rb = new JRadioButton("Pontual", false);
		rb.addActionListener(this);
		rb.setActionCommand("MP");
		bg.add(rb);
		p.add(rb);

		p.add(new JSeparator(JSeparator.VERTICAL));

		bg = new ButtonGroup();

		p0 = new JRadioButton("Faixa de tempo", true);
		p0.addActionListener(this);
		p0.setActionCommand("0");
		bg.add(p0);
		p.add(p0);

		p1 = new JRadioButton("Últimos dias", false);
		p1.addActionListener(this);
		p1.setActionCommand("1");
		bg.add(p1);
		p.add(p1);

		p2 = new JRadioButton("Tudo", true);
		p2.addActionListener(this);
		p2.setActionCommand("2");
		bg.add(p2);
		p.add(p2);

		Calendar c = Calendar.getInstance();
		Calendar c0 = (Calendar) c.clone();
		c0.add(Calendar.DAY_OF_MONTH, -10);
		pi = new PeriodInput(new GregorianCalendar(1900, 0, 1), c, c0, c, false, 0, false);
		p.add(pi);

		ti = new TimeInput(c0, false);
		ti.setVisible(false);
		p.add(ti);

		this.dias = new JSpinner(new SpinnerNumberModel(1, 1, 30, 1));
		dias.setVisible(false);
		p.add(dias);
		p.add(this.dl = new JLabel("dias"));
		dl.setVisible(false);

		add(p, BorderLayout.NORTH);

		// -----------------------------------------------------

		this.med = new JPanel(new CardLayout());

		this.chart = new MeasChart<>(700, 400);
		this.med.add(this.chart, "PE");

		this.med.add(
				new JScrollPane(
						new JTable(atm = new AdvancedTableModel(new Object[] { "Cotação", "Data", "Valor" }, 0))),
				"MP");

		add(med, BorderLayout.CENTER);

		// -----------------------------------------------------

		Grade g = new Grade();

		this.fl = new FilterableList<>();
		g.add(fl, 0, 0, 2, 1);
		fl.setPreferredSize(new Dimension(-1, 200));
		{
			String[] ss = IOutils.readFile2(new File("cotacoes.txt")).split("\n");
			for (int i = 3; i < ss.length; i++)
				fl.addElement(ss[i].substring(8));
		}

		JButton b = new JButton("\u25BC");
		b.setPreferredSize(new Dimension(50, 25));
		b.addActionListener(this);
		b.setActionCommand("a");
		g.add(b, 0, 1, 1, 1);

		b = new JButton("\u25B2");
		b.setPreferredSize(new Dimension(50, 25));
		b.addActionListener(this);
		b.setActionCommand("d");
		g.add(b, 1, 1, 1, 1);

		this.list = new JList<>(dlm = new DefaultListModel<>());
		g.add(new JScrollPane(list), 0, 2, 2, 1);

		b = new JButton("Atualizar");
		b.addActionListener(this);
		b.setActionCommand("R");
		g.add(b, 0, 3, 2, 1);

		add(g, BorderLayout.WEST);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		String command = event.getActionCommand();
		switch (command) {
		case "a":
			String tag = this.fl.get();
			if (tag != null)
				dlm.addElement(tag);
			break;
		case "d":
			List<String> selected = list.getSelectedValuesList();
			for (String s : selected)
				dlm.removeElement(s);
			break;
		case "R":
			if (dlm.getSize() > 0) {
				int[] indices = new int[dlm.getSize()];
				for (int i = 0; i < dlm.getSize(); i++)
					indices[i] = Integer.parseInt(dlm.get(i).split(" => ")[0]);

				BacenXMLreader br = new BacenXMLreader(indices);
				if (pe.isSelected()) {

					if (p0.isSelected()) // período dado
						br.read(pi.get());
					else // últimos dias
						br.read((int) dias.getValue());

					RegP med = br.get1();
					chart.clear();
					for (int i = 0; i < med.length(); i++)
						chart.put(indices[i], ColorUtils.getColor(i), med.getMap(i));
				} else {
					if (p0.isSelected()) // um dado dia
						br.read(ti.get());
					else // último dia
						br.read();

					Med[] med = br.get2();
					atm.setRowCount(med.length);
					for (int i = 0; i < med.length; i++) {
						if (med[i] != null) {
							atm.setValueAt(indices[i], i, 0);
							atm.setValueAt(String.format("%1$td/%1$tm/%1$tY", med[i].getTime()), i, 1);
							atm.setValueAt(med[i].getValue(), i, 2);
						} else {
							atm.setValueAt(indices[i], i, 0);
							atm.setValueAt(null, i, 1);
							atm.setValueAt(null, i, 2);
						}
					}
				}
			}
			break;
		case "0":
			pi.setVisible(pe.isSelected());
			ti.setVisible(!pe.isSelected());
			dias.setVisible(false);
			dl.setVisible(false);
			break;
		case "1":
			pi.setVisible(false);
			ti.setVisible(false);
			dias.setVisible(pe.isSelected());
			dl.setVisible(pe.isSelected());
			break;
		default:
			((CardLayout) this.med.getLayout()).show(this.med, command);
			if ("PE".equals(command)) {
				p0.setText("Faixa de tempo");
				p1.setText("Últimos dias");
				pi.setVisible(ti.isVisible());
				if (ti.isVisible())
					ti.setVisible(false);
				else {
					dias.setVisible(true);
					dl.setVisible(true);
				}
			} else {
				p0.setText("Determinado dia");
				p1.setText("Último dia");
				ti.setVisible(pi.isVisible());
				if (pi.isVisible())
					pi.setVisible(false);
				else {
					dias.setVisible(false);
					dl.setVisible(false);
				}
			}
			break;
		}
	}
}
