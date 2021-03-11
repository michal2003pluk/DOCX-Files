import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;

public class WordDocument {

	static String workingDir = "";

	public static void main(String[] args) throws InvalidFormatException, IOException, XmlException {

		ArrayList<sqlRow> data = extractSQL("jdbc:mysql://freedb.tech:3306/freedbtech_dbMikey",
				"freedbtech_ThunderCandy", "password", "tblPizza");

//		for (sqlRow node : data) { // Debug
//			System.out.format("%s, %s, %s, %s \n", node.get(0), node.get(1), node.get(2), node.get(3));
//		}

		workingDir = getCurrentDir();
		String templatePath = workingDir + "\\src\\Resources\\Template.docx";

		generateDocument(templatePath, workingDir + "\\src\\OutputDocuments\\out2.docx", data);

	}

	public static ArrayList<sqlRow> extractSQL(String url, String username, String password, String tableName) {

		ArrayList<sqlRow> tempArr = new ArrayList<sqlRow>();

		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			Connection conn = DriverManager.getConnection(url, username, password);
			System.out.println("Connected to database");

			String SQL = "SELECT * FROM " + tableName;
			Statement statement;
			ResultSet result;

			statement = conn.createStatement();
			result = statement.executeQuery(SQL);

			while (result.next()) {
				sqlRow tempRow = new sqlRow(result.getString("PizzaType"), result.getString("Toppings"),
						result.getString("Cost"), result.getString("SpecialInfo"));
				tempArr.add(tempRow);
			}

		} catch (SQLException e) {
			System.out.println("SQLException: " + e.toString());
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found exception: " + e.toString());
		}

		return tempArr;
	}

	public static String getCurrentDir() {
		Path currentRelativePath = Paths.get("");
		return currentRelativePath.toAbsolutePath().toString();
	}

	public static void generateDocument(String source, String destination, ArrayList<sqlRow> data)
			throws InvalidFormatException, IOException, XmlException {

		XWPFDocument doc = new XWPFDocument(OPCPackage.open(source));
		doc = resizeDocumentTable(doc, data.size());

		saveDocument(doc, destination);

	}

	public static XWPFDocument resizeDocumentTable(XWPFDocument document, int amtRows)
			throws XmlException, IOException {

		List<XWPFTable> tables = document.getTables();
		XWPFTable table = tables.get(0);

		XWPFTableRow blankRow = table.getRows().get(2);

		if (amtRows == 0) {
			table.removeRow(2);
			table.removeRow(1);

		} else if (amtRows == 1) {
			table.removeRow(2);

		} else if (amtRows > 2) {
			int newRowsNeeded = amtRows - 2;
			int startRowInsert = 2;
			for (int i = 2; i < newRowsNeeded + 2; i++) {
				CTRow ctrow = CTRow.Factory.parse(blankRow.getCtRow().newInputStream());
				XWPFTableRow newRow = new XWPFTableRow(ctrow, table);

				int cellIndex = 0;
				for (XWPFTableCell cell : newRow.getTableCells()) {
					for (XWPFParagraph paragraph : cell.getParagraphs()) {
						for (XWPFRun run : paragraph.getRuns()) {
							run.setText("$e", 0);
						}
					}
				}
				table.addRow(newRow, startRowInsert++);
			}
		} else {
			System.out.println("Not a valid amount of rows specified");
		}

		return document;
	}

	public static void saveDocument(XWPFDocument document, String destination) {
		try {
			document.write(new FileOutputStream(destination));
		} catch (IOException e) {
			System.out.println("Error saving file");
			e.printStackTrace();
		}
	}

}
