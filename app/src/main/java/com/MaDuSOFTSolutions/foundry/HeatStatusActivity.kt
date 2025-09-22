package com.MaDuSOFTSolutions.foundry

// Add this import at the top of the file
// Add this import at the top of the file
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import java.io.Serializable
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.text.DateFormat


// Update your data classes to implement Serializable
data class DetailSection(
    val title: String,
    val fields: List<DetailField> = emptyList(),
    val content: String = ""  // Add this line for raw text content
) : Serializable
data class DetailField(val label: String, val value: String) : Serializable
data class CombinedData(
    val mainData: List<DetailField>,
    val detailedSections: List<DetailSection>
) : Serializable

class HeatStatusActivity : AppCompatActivity() {


    private lateinit var progressDialog: AlertDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heat_status)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val inputHeatNo = findViewById<TextInputEditText>(R.id.inputHeatNo)
        val button = findViewById<Button>(R.id.button2)

        button.setOnClickListener {
            val heatNo = inputHeatNo.text.toString().trim()
            if (heatNo.isEmpty()) {
                Toast.makeText(this, "Please enter Heat No", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoadingDialog("Fetching data...")

            Thread {
                val conn = getConnection()
                if (conn == null) {
                    runOnUiThread {
                        hideLoadingDialog()
                        Toast.makeText(this, "Database connection failed", Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }

                val query = """
                SELECT DISTINCT 
                    P3.SHORTNAME AS CUSTOMER_NAME,
                    P1.ITEM_NAME,
                    P1.DRAWING_NO,
                    P4.SHORTNAME AS GRADE,
                    PSD.QUANTITY,
                    PSD.SLNO
                FROM PRPOUR_DETAILS AS PSD
                LEFT JOIN PRITEMMASTER AS P1 ON PSD.ITEM_ID = P1.ITEM_ID
                LEFT JOIN ACHOA AS P3 ON PSD.CUST_ID = P3.ACCODE
                LEFT JOIN PRGRADEMASTER AS P4 ON PSD.GRADE_ID = P4.GRADE_ID
                WHERE LTRIM(RTRIM(PSD.HEAT_NO)) = ?
                ORDER BY PSD.SLNO
            """.trimIndent()

                try {
                    val stmt = conn.prepareStatement(query)
                    stmt.setString(1, heatNo)
                    val rs = stmt.executeQuery()
                    val rsmd = rs.metaData
                    val columnCount = rsmd.columnCount

                    val headers = mutableListOf<String>()
                    val rows = mutableListOf<List<String>>()
                    val columnLabelMap = mapOf(
                        "CUSTOMER_NAME" to "Cust. Name",
                        "ITEM_NAME" to "Item Name",
                        "DRAWING_NO" to "Drawing No",
                        "GRADE" to "Grade Name",
                        "QUANTITY" to "Qty",
                        "SLNO" to "Sl. No"
                    )

                    for (i in 1..columnCount) {
                        val colName = rsmd.getColumnLabel(i)
                        val label = columnLabelMap[colName]
                            ?: colName.replace("_", " ").replaceFirstChar(Char::uppercase)
                        headers.add(label)
                    }

                    while (rs.next()) {
                        val row = mutableListOf<String>()
                        for (i in 1..columnCount) {
                            row.add(rs.getString(i) ?: "")
                        }
                        rows.add(row)
                    }

                    runOnUiThread {
                        hideLoadingDialog()
                        if (rows.isEmpty()) {
                            Toast.makeText(this, "No data found for $heatNo", Toast.LENGTH_LONG).show()
                        } else {
                            recyclerView.layoutManager = GridLayoutManager(this, columnCount)
                            recyclerView.adapter = TableCellAdapter(headers, rows) { clickedRow ->
                                val rowData = rows[clickedRow]
                                // Build the summary fields
                                val summaryFields = headers.zip(rowData)
                                    .filterNot { (label, _) -> label.equals("Sl. No", ignoreCase = true) }
                                    .toMutableList()

// Add Heat No
                                summaryFields.add(0, "Heat No" to heatNo)

// Placeholder for casting weight and section thickness

// Fetch these two from your `fetchDetailedData()` result later
// For now, add dummy placeholders; you'll replace them in that method if needed
                                summaryFields.add("Casting Weight" to "Loading...")
                                summaryFields.add("Section Thickness" to "Loading...")

                                val mainFields = summaryFields.map { (label, value) ->
                                    DetailField(label, value)
                                }

                                val slno = rowData[columnCount - 1]
                                fetchDetailedData(heatNo, slno, mainFields)
                            }
                        }
                    }

                    rs.close()
                    stmt.close()
                    conn.close()

                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        hideLoadingDialog()
                        Toast.makeText(this, "Query error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }
    }




    private fun fetchDetailedData(heatNo: String, slno: String,mainFields: List<DetailField>) {
        showLoadingDialog("Fetching details...")
        Thread {

            var hslno = slno.toIntOrNull() ?: 0
            var searchstring = heatNo.uppercase()


            var query1:String?
            var Tgradeid:Int=0
            var Tworderno:String=""
            var Tworderfinyear:String=""
            var Tposlno:String=""
            var PourStg:String=""
            var PrdnStg:String=""
            var heatfound: Boolean = false
            var Titemid:Int=0
            var Tcustid:Int=0
            var TestReqd:String=""
            var ItemStg:String=""
            var ScStg:String=""
            var WoStg:String=""
            var FettStg:String=""
            val myresult6 = "Heat Number : " + searchstring + "\n"

            var conn: Connection? = null
            var prepStmt: PreparedStatement? = null
            var prepStmt1: PreparedStatement? = null
            var prepStmt2: PreparedStatement? = null
            var prepStmt3: PreparedStatement? = null
            var rs: ResultSet? = null
            var rs1: ResultSet? = null
            var rs2: ResultSet? = null
            var rs3: ResultSet? = null

            try {
                conn = getConnection()
                if (conn == null) {
                    runOnUiThread {
                        Toast.makeText(this, "DB connection failed", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                var query =
                    ("select top 1 P3.SHORTNAME AS ASHORTNAME,P1.ITEM_NAME,P1.DRAWING_NO,P1.MACH_DWG_NO,P1.RAW_DWG_NO,P2.SHORTNAME AS GSHORTNAME,P1.CASTING_WGT, PSD.QUANTITY,PSD.HEAT_NO,PSD.CUST_ID,PSD.ITEM_ID,PSD.GRADE_ID,PSD.WORDER_NO,PSD.WORDER_FIN_YEAR,PSD.POSLNO,ISNULL(P4.RT_QTY,0) AS RT_QTY,P4.RT_REQD,RT_CONDITION,NDT_REMARKS, "
                            + " P5.CUST_ORDER_NO,P5.CUST_ORDER_DATE,P4.QUANTITY AS POQTY,PSD1.POUR_DATE,PSD1.TAPP_TEMP,PSD1.TAPPING_TIME AS TAPP_TIME,P4.CUST_DELY_DATE,P4.DELIVERY_CONDITION,  "
                            + " P4.IBR_REQD, P4.TEST31_REQD, P4.TEST32_REQD, P4.TEST22_REQD, P4.UT_REQD, P4.MPI_REQD, P4.DP_REQD, P4.IMPACT_REQD, P4.IGC_REQD, "
                            + " PSD1.OPERATOR, PSD1.SUPERVISOR, PSD1.CLOSING_SUPERVISOR, P1.SECTION_THICKNESS, "
                            + " (SELECT SUM(P4.HEAT_QTY) FROM SAINVOICE_HEAT_DETAILS AS P4 WHERE P4.HEAT_NO = PSD.HEAT_NO AND P4.CUST_ID = PSD.CUST_ID "
                            + " AND P4.ITEM_ID = PSD.ITEM_ID AND P4.GRADE_ID = PSD.GRADE_ID AND P4.WORDER_NO = PSD.WORDER_NO AND P4.WORDER_FIN_YEAR = PSD.WORDER_FIN_YEAR "
                            + " AND P4.POSLNO = PSD.POSLNO) AS INVQTY, "
                            + " (SELECT SUM(ISNULL(P4.REJECT_QTY,0)) FROM PRINSP_DETAILS AS P4 WHERE P4.HEAT_NO = PSD.HEAT_NO AND P4.CUST_ID = PSD.CUST_ID "
                            + " AND P4.ITEM_ID = PSD.ITEM_ID AND P4.GRADE_ID = PSD.GRADE_ID AND P4.WORDER_NO = PSD.WORDER_NO AND P4.WORDER_FIN_YEAR = PSD.WORDER_FIN_YEAR "
                            + " AND P4.POSLNO = PSD.POSLNO AND ISNULL(P4.REJECT_TYPE,'') <> '8') AS REJECTQTY, "
                            + " (SELECT SUM(ISNULL(P4.QUANTITY,0)) FROM PRKOOFF_DETAILS AS P4 WHERE P4.HEAT_NO = PSD.HEAT_NO AND P4.CUST_ID = PSD.CUST_ID "
                            + " AND P4.ITEM_ID = PSD.ITEM_ID AND P4.GRADE_ID = PSD.GRADE_ID AND P4.WORDER_NO = PSD.WORDER_NO AND P4.WORDER_FIN_YEAR = PSD.WORDER_FIN_YEAR "
                            + " AND P4.POSLNO = PSD.POSLNO ) AS KOQTY, "
                            + " (SELECT SUM(ISNULL(P4.QUANTITY,0)) FROM PRHTOFF_DETAILS AS P4 WHERE P4.HEAT_NO = PSD.HEAT_NO AND P4.CUST_ID = PSD.CUST_ID "
                            + " AND P4.ITEM_ID = PSD.ITEM_ID AND P4.GRADE_ID = PSD.GRADE_ID AND P4.WORDER_NO = PSD.WORDER_NO AND P4.WORDER_FIN_YEAR = PSD.WORDER_FIN_YEAR "
                            + " AND P4.POSLNO = PSD.POSLNO ) AS HTQTY, "
                            + " (SELECT SUM(ISNULL(P4.QUANTITY,0)) FROM PRFETOFF_DETAILS AS P4 WHERE P4.HEAT_NO = PSD.HEAT_NO AND P4.CUST_ID = PSD.CUST_ID "
                            + " AND P4.ITEM_ID = PSD.ITEM_ID AND P4.GRADE_ID = PSD.GRADE_ID AND P4.WORDER_NO = PSD.WORDER_NO AND P4.WORDER_FIN_YEAR = PSD.WORDER_FIN_YEAR "
                            + " AND P4.POSLNO = PSD.POSLNO ) AS FETTQTY, "
                            + " (SELECT SUM(ISNULL(P4.HEAT_QTY,0)) FROM PRDESPADV_HEAT_DETAILS AS P4 LEFT OUTER JOIN PRDESPADV_HEADER AS P5 ON P4.ADVISE_NO = P5.ADVISE_NO AND P4.FIN_YEAR = P5.FIN_YEAR  WHERE P4.HEAT_NO = PSD.HEAT_NO AND P4.CUST_ID = PSD.CUST_ID "
                            + " AND P4.ITEM_ID = PSD.ITEM_ID AND P4.GRADE_ID = PSD.GRADE_ID AND P4.WORDER_NO = PSD.WORDER_NO AND P4.WORDER_FIN_YEAR = PSD.WORDER_FIN_YEAR "
                            + " AND P4.POSLNO = PSD.POSLNO AND ISNULL(P5.CLOSED,0)=0 ) AS DAQTY, "
                            + " (SELECT SUM(HEAT_QTY) FROM PRDCOUTHEAT_DETAILS AS APSD LEFT OUTER JOIN PRDCOUT_HEADER AS APSD1 ON APSD.DC_NUMBER=APSD1.DC_NUMBER AND APSD.FIN_YEAR = APSD1.FIN_YEAR WHERE ISNULL(APSD1.DC_CLOSED,0)=0 AND APSD.HEAT_NO = PSD.HEAT_NO AND APSD.ITEM_ID =  PSD.ITEM_ID AND APSD.GRADE_ID = PSD.GRADE_ID AND APSD.WORDER_NO = PSD.WORDER_NO AND APSD.WORDER_FIN_YEAR = PSD.WORDER_FIN_YEAR AND APSD.POSLNO = PSD.POSLNO ) AS DCOUTQTY,  "
                            + " (SELECT SUM(HEAT_QTY) FROM PRDCINHEAT_DETAILS AS APSD LEFT OUTER JOIN PRDCIN_HEADER AS APSD1 ON APSD.INWARD_NUMBER=APSD1.INWARD_NUMBER AND APSD.FIN_YEAR = APSD1.FIN_YEAR  LEFT OUTER JOIN PRDCOUT_HEADER AS APSD2 ON APSD1.DC_NUMBER=APSD2.DC_NUMBER AND APSD1.DC_FIN_YEAR = APSD2.FIN_YEAR  WHERE APSD.HEAT_NO = PSD.HEAT_NO AND APSD.ITEM_ID =  PSD.ITEM_ID AND APSD.GRADE_ID = PSD.GRADE_ID AND APSD.WORDER_NO = PSD.WORDER_NO AND APSD.WORDER_FIN_YEAR = PSD.WORDER_FIN_YEAR AND APSD.POSLNO = PSD.POSLNO ) AS DCINQTY  "
                            + " from PRPOUR_DETAILS AS PSD "
                            + " LEFT OUTER JOIN PRITEMMASTER  AS P1 ON PSD.ITEM_ID = P1.ITEM_ID "
                            + " LEFT OUTER JOIN PRGRADEMASTER AS P2 ON PSD.GRADE_ID=P2.GRADE_ID "
                            + " LEFT OUTER JOIN ACHOA AS P3 ON PSD.CUST_ID = P3.ACCODE "
                            + " LEFT OUTER JOIN SAWORK_DETAILS AS P4 ON PSD.WORDER_NO = P4.WORDER_NO AND PSD.WORDER_FIN_YEAR = P4.FIN_YEAR AND PSD.POSLNO = P4.POSLNO AND PSD.ITEM_ID = P4.ITEM_ID AND PSD.GRADE_ID = P4.GRADE_ID"
                            + " LEFT OUTER JOIN SAWORK_HEADER AS P4A ON P4.WORDER_NO = P4A.WORDER_NO AND P4.FIN_YEAR = P4A.FIN_YEAR "
                            + " LEFT OUTER JOIN SAORDER_HEADER AS P5 ON P4A.ORDER_NO = P5.ORDER_NO AND P4A.ORDER_FIN_YEAR = P5.FIN_YEAR "
                            + " LEFT OUTER JOIN PRPOUR_HEADER AS PSD1 ON PSD.HEAT_NO = PSD1.HEAT_NO "
                            + " where LTRIM(RTRIM(PSD.HEAT_NO)) = ?  ")




                prepStmt = conn.prepareStatement(query + " AND PSD.SLNO = ?").apply {
                    var paramIndex = 1
                    setString(paramIndex++, heatNo)
                    if (hslno > 0) {
                        setInt(paramIndex, hslno)
                    }
                }

                rs = prepStmt.executeQuery()






                var myresult = myresult6;
                val details = mutableListOf<DetailField>()


                while (rs.next()) {

                    Titemid = rs.getInt("ITEM_ID");
                    Tcustid = rs.getInt("CUST_ID");
                    Tgradeid = rs.getInt("GRADE_ID");
                    Tworderno = rs.getString("WORDER_NO");
                    Tworderfinyear = rs.getString("WORDER_FIN_YEAR");
                    Tposlno = rs.getString("POSLNO");

                    val sectionThickness = rs.getString("SECTION_THICKNESS")
                    val castingWgt      = rs.getString("CASTING_WGT")

                    // Populate the details list so the dialog can pick them up
                    details.add( DetailField(label = "Section Thickness", value = sectionThickness) )
                    details.add( DetailField(label = "Casting Weight"   , value = castingWgt)         )


                    Log.d("Titemid",Titemid.toString())


//String rejectqty = rs.getString("REJECTQTY").isnull;
//if (rejectqty.wasNull())
//{rejectqty = "";}
                    heatfound = true;
                    val custOrderDateTime: java.sql.Timestamp? = rs.getTimestamp("CUST_ORDER_DATE")
                    val formattedDate = DateFormat.getDateInstance().format(custOrderDateTime)

//myresult = String.format(myresult + "Pouring Date : " + rs.getDate("POUR_DATE"));
                    myresult = String.format(
                        myresult + "Pouring Date : " + android.text.format.DateFormat.format(
                            "dd-MM-yyyy",
                            rs.getDate("POUR_DATE")
                        )
                    );
                    ItemStg = String.format(
                        ItemStg + "Pouring Date : " + android.text.format.DateFormat.format(
                            "dd-MM-yyyy",
                            rs.getDate("POUR_DATE")
                        )
                    );

                    myresult = myresult + "\nCustomer : " + rs.getString("ASHORTNAME") +
                            "\nItem Name : " + rs.getString("ITEM_NAME") +
                            "\nDrawing No. : " + rs.getString("DRAWING_NO") +
                            "\nRough Drawing No. : " + rs.getString("RAW_DWG_NO") +
                            "\nMachining Drawing No. : " + rs.getString("MACH_DWG_NO") +
                            "\nSection Thickness : " + rs.getString("SECTION_THICKNESS") +
                            "\nGrade : " + rs.getString("GSHORTNAME") +
                            "\nCustomer PO NO. : " + rs.getString("CUST_ORDER_NO").trim() +
                            " [" + android.text.format.DateFormat.format(
                        "dd-MM-yyyy",
                        rs.getDate("CUST_ORDER_DATE")
                    ) + "]" +
                            "  Sl.#" + rs.getString("POSLNO").trim() + "]" +
                            "\nWork Order NO. : " + rs.getString("WORDER_NO").trim() +
                            " [" + rs.getString("WORDER_FIN_YEAR") + "]" +
                            "\nPO Qty. : " + rs.getString("POQTY") +
                            "\nDelivery Condition : " + rs.getString("DELIVERY_CONDITION") +
                            "\nCust. Delivery Date : " + android.text.format.DateFormat.format(
                        "dd-MM-yyyy",
                        rs.getDate("CUST_DELY_DATE")
                    ) +
                            "\nCasting Wgt. : " + rs.getString("CASTING_WGT")

                    ItemStg = ItemStg + "\nCustomer : " + rs.getString("ASHORTNAME")
                    "\nItem Name : " + rs.getString("ITEM_NAME") +
                            "\nDrawing No. : " + rs.getString("DRAWING_NO") +
                            "\nRough Drawing No. : " + rs.getString("RAW_DWG_NO") +
                            "\nMachining Drawing No. : " + rs.getString("MACH_DWG_NO") +
                            "\nSection Thickness : " + rs.getString("SECTION_THICKNESS") +
                            "\nGrade : " + rs.getString("GSHORTNAME") +
                            "\nCasting Wgt. : " + rs.getString("CASTING_WGT")

                    WoStg = WoStg + "\nCustomer PO NO. : " + rs.getString("CUST_ORDER_NO").trim() +
                            " [" + android.text.format.DateFormat.format(
                        "dd-MM-yyyy",
                        rs.getDate("CUST_ORDER_DATE")
                    ) + "]" +
                            "  Sl.#" + rs.getString("POSLNO").trim() + "]" +
                            "\nWork Order NO. : " + rs.getString("WORDER_NO").trim() +
                            " [" + rs.getString("WORDER_FIN_YEAR") + "]" +
                            "\nPO Qty. : " + rs.getString("POQTY") +
                            "\nDelivery Condition : " + rs.getString("DELIVERY_CONDITION")
// Commented out line for now
// + "\nCust. Delivery Date : " + android.text.format.DateFormat.format("dd-MM-yyyy", rs.getDate("CUST_DELY_DATE"))


                    if (rs.getBoolean("IBR_REQD")) {
                        TestReqd = TestReqd + " IBR";
                    }
                    if (rs.getBoolean("TEST31_REQD")) {
                        TestReqd = TestReqd + " 3.1";
                    }
                    if (rs.getBoolean("TEST32_REQD")) {
                        TestReqd = TestReqd + " 3.2";
                    }
                    if (rs.getBoolean("TEST22_REQD")) {
                        TestReqd = TestReqd + " 2.2";
                    }
                    if (rs.getBoolean("RT_REQD")) {
                        TestReqd = TestReqd + " RT";
                    }
                    if (rs.getBoolean("UT_REQD")) {
                        TestReqd = TestReqd + " UR";
                    }
                    if (rs.getBoolean("MPI_REQD")) {
                        TestReqd = TestReqd + " MPI";
                    }
                    if (rs.getBoolean("DP_REQD")) {
                        TestReqd = TestReqd + " DP";
                    }
                    if (rs.getBoolean("IMPACT_REQD")) {
                        TestReqd = TestReqd + " IMPACT";
                    }
                    if (rs.getBoolean("IGC_REQD")) {
                        TestReqd = TestReqd + " IGC";
                    }

                    if (TestReqd.length > 0) {
                        myresult = myresult + "\nTest Requirement : " + TestReqd;
                        WoStg = WoStg + "\nTest Requirement : " + TestReqd;
                    }
                    if (rs.getInt("RT_QTY") > 0) {
                        myresult = myresult + "\nRT Reqd. Qty. : " + rs.getString("RT_QTY");
                        WoStg = WoStg + "\nRT Reqd. Qty. : " + rs.getString("RT_QTY");
                    }
                    if (rs.getString("RT_CONDITION").length > 0) {
                        myresult = myresult + "\nRT Condition : " + rs.getString("RT_CONDITION");
                        WoStg = WoStg + "\nRT Condition : " + rs.getString("RT_CONDITION");
                    }
                    if (rs.getString("NDT_REMARKS").length > 0) {
                        myresult = myresult + "\nNDT Requirements : " + rs.getString("NDT_REMARKS");
                        WoStg = WoStg + "\nNDT Requirements : " + rs.getString("NDT_REMARKS");
                    }
                    myresult = myresult + "\nPoured Qty. : " + rs.getString("QUANTITY");
                    PourStg = PourStg + "\nPoured Qty. : " + rs.getString("QUANTITY");
                    if (rs.getInt("REJECTQTY") > 0) {
                        myresult = myresult + "\nReject Qty. : " + rs.getString("REJECTQTY");
                        WoStg = WoStg + "\nReject Qty. : " + rs.getString("REJECTQTY");
                    }
                    if (rs.getInt("KOQTY") > 0) {
                        myresult = myresult + "\nKnock Out Qty. : " + rs.getString("KOQTY");
                        PrdnStg = PrdnStg + "\nKnock Out Qty. : " + rs.getString("KOQTY");
                    }
                    if (rs.getInt("HTQTY") > 0) {
                        myresult = myresult + "\nHeat Treatment Qty. : " + rs.getString("HTQTY");
                        PrdnStg = PrdnStg + "\nHeat Treatment Qty. : " + rs.getString("HTQTY");
                    }
                    if (rs.getInt("FETTQTY") > 0) {
                        myresult = myresult + "\nFettling Qty. : " + rs.getString("FETTQTY");
                        PrdnStg = PrdnStg + "\nFettling Qty. : " + rs.getString("FETTQTY");
                    }
                    if (rs.getInt("DCOUTQTY") > 0) {
                        myresult = myresult + "\nDC Out Qty. : " + rs.getString("DCOUTQTY");
                        PrdnStg = PrdnStg + "\nDC Out Qty. : " + rs.getString("DCOUTQTY");
                    }
                    if (rs.getInt("DCINQTY") > 0) {
                        myresult = myresult + "\nDC In Qty. : " + rs.getString("DCINQTY");
                        PrdnStg = PrdnStg + "\nDC In Qty. : " + rs.getString("DCINQTY");
                    }
                    if (rs.getInt("DAQTY") > 0) {
                        myresult = myresult + "\nDespatch Advise Qty. : " + rs.getString("DAQTY");
                        PrdnStg = PrdnStg + "\nDespatch Advise Qty. : " + rs.getString("DAQTY");
                    }
                    if (rs.getInt("INVQTY") > 0) {
                        myresult = myresult + "\nInvoice Qty. : " + rs.getString("INVQTY");
                        PrdnStg = PrdnStg + "\nInvoice Qty. : " + rs.getString("INVQTY");
                    }
                    myresult = myresult + "\nPouring Parameters : ";
                    myresult = myresult + "\nTapping Temp : " + rs.getString("TAPP_TEMP");
                    myresult = myresult + "\nOperator : " + rs.getString("OPERATOR");
                    myresult = myresult + "\nSupervisor : " + rs.getString("SUPERVISOR");
                    myresult = myresult + "\nClosing Supervisor : " + rs.getString("CLOSING_SUPERVISOR");

//PourStg = PourStg + "\nPouring Parameters : ";
                    PourStg = PourStg + "\nTapping Temp : " + rs.getString("TAPP_TEMP");
                    PourStg = PourStg + "\nTapping Time : " + rs.getDouble("TAPP_TIME");
                    PourStg = PourStg + "\nOperator : " + rs.getString("OPERATOR");
                    PourStg = PourStg + "\nSupervisor : " + rs.getString("SUPERVISOR");
                    PourStg = PourStg + "\nClosing Supervisor : " + rs.getString("CLOSING_SUPERVISOR");

                    myresult = myresult + "\n";
                    //Log.w("Data:",myresult );
//              Log.w("Data",rs.getString(2));
                }
                Log.d("heat",heatfound.toString())

                if (heatfound == false) {
                    myresult = myresult + "Heat Number Not Found";
                    Log.d("HEAT_NOT_FOUND", myresult)
                }


                if (heatfound == true) {

                    Log.d("HEAT_NOT_FOUND_Quer1", myresult)
                    Log.d("searching",searchstring)

                    query1 =
                        "SELECT DISTINCT DC_NUMBER,DC_DATE, HOA,PROCESS_NAME,OUTQTY,INQTY, OUTQTY-INQTY AS BALQTY FROM " +
                                " (SELECT DISTINCT DC_NUMBER,DC_DATE, HOA,PROCESS_NAME, SUM(ISNULL(OUTQTY,0)) AS OUTQTY, SUM(ISNULL(INQTY,0)) AS INQTY FROM " +
                                " ((SELECT DISTINCT APSD.DC_NUMBER,APSD1.DC_DATE,HOA,PROCESS_NAME,SUM(HEAT_QTY) AS OUTQTY, 0 AS INQTY FROM PRDCOUTHEAT_DETAILS AS APSD " +
                                " LEFT OUTER JOIN PRDCOUT_HEADER AS APSD1 ON APSD.DC_NUMBER=APSD1.DC_NUMBER AND APSD.FIN_YEAR = APSD1.FIN_YEAR " +
                                " LEFT OUTER JOIN ACHOA AS PSD2 ON APSD1.PARTY_ID= PSD2.ACCODE " +
                                " LEFT OUTER JOIN STPROCESS AS PSD3 ON APSD1.PROCESS_ID = PSD3.PROCESS_ID " +
                                " WHERE ISNULL(APSD1.DC_CLOSED,0)=0 AND " +
                                " LTRIM(RTRIM(APSD.HEAT_NO)) = '" + searchstring + "' " +
                                " AND APSD.ITEM_ID = " + Titemid + " AND APSD.GRADE_ID = " + Tgradeid +
                                " AND APSD.WORDER_NO = '" + Tworderno + "' AND APSD.WORDER_FIN_YEAR = '" + Tworderfinyear + "'" +
                                " AND APSD.POSLNO = '" + Tposlno + "' GROUP BY APSD.DC_NUMBER,APSD1.DC_DATE,HOA,PROCESS_NAME)   " +
                                " UNION ALL " +
                                " (SELECT DISTINCT APSD1.DC_NUMBER,APSD2.DC_DATE,HOA,PROCESS_NAME, 0 AS OUTQTY, SUM(HEAT_QTY)  AS INQTY FROM PRDCINHEAT_DETAILS AS APSD " +
                                " LEFT OUTER JOIN PRDCIN_HEADER AS APSD1 ON APSD.INWARD_NUMBER=APSD1.INWARD_NUMBER AND APSD.FIN_YEAR = APSD1.FIN_YEAR  " +
                                " LEFT OUTER JOIN PRDCOUT_HEADER AS APSD2 ON APSD1.DC_NUMBER=APSD2.DC_NUMBER AND APSD1.DC_FIN_YEAR = APSD2.FIN_YEAR  " +
                                " LEFT OUTER JOIN ACHOA AS PSD2 ON APSD1.PARTY_ID= PSD2.ACCODE " +
                                " LEFT OUTER JOIN STPROCESS AS PSD3 ON APSD2.PROCESS_ID = PSD3.PROCESS_ID " +
                                " WHERE " +
                                " LTRIM(RTRIM(APSD.HEAT_NO)) = '" + searchstring + "' " +
                                " AND APSD.ITEM_ID = " + Titemid + " AND APSD.GRADE_ID = " + Tgradeid +
                                " AND APSD.WORDER_NO = '" + Tworderno + "' AND APSD.WORDER_FIN_YEAR = '" + Tworderfinyear + "'" +
                                " AND APSD.POSLNO = '" + Tposlno + "' GROUP BY APSD1.DC_NUMBER,APSD2.DC_DATE,HOA,PROCESS_NAME)   " +
                                ") AS Q1 GROUP BY DC_NUMBER, DC_DATE, HOA,PROCESS_NAME) AS Q1 WHERE ISNULL(OUTQTY,0)-ISNULL(INQTY,0)>=0 ORDER BY HOA,DC_NUMBER ";
                    prepStmt1 = conn.prepareStatement(query1);
//prepStmt = conn.prepareStatement(query1);
// prepStmt.setString( 1, searchstring);
//Log.w("Query : :",prepStmt.toString() );
                    rs1 = prepStmt1.executeQuery();
                    Log.d("rs1", prepStmt1.toString())
                    while (rs1.next()) {

                        val dcNumber = rs1.getString("DC_NUMBER")
                        val dcdate = rs1.getString("DC_DATE")
                        val hoa = rs1.getString("HOA")
                        val processName = rs1.getString("PROCESS_NAME")
                        val outQty = rs1.getInt("OUTQTY")
                        val inQty = rs1.getInt("INQTY")
                        val balQty = rs1.getInt("BALQTY")

                        Log.d(
                            "QueryResult", "DC_NUMBER: " + dcNumber +
                                    "DC_DATE:"+dcdate+
                                    ", HOA: " + hoa +
                                    ", PROCESS_NAME: " + processName +
                                    ", OUTQTY: " + outQty +
                                    ", INQTY: " + inQty +
                                    ", BALQTY: " + balQty
                        )
                        Log.d("SUBCONTRACT_out", rs1.metaData.toString())

                        myresult = myresult + "\nS.C. Name : " + rs1.getString("HOA") +
                                "\nProcess Name : " + rs1.getString("PROCESS_NAME") +
                                "\nDC Out Quantity : " + rs1.getInt("OUTQTY") +
                                "\nDC In Quantity : " + rs1.getInt("INQTY") +
                                "\nStock : " + rs1.getInt("BALQTY");

                        ScStg =  ScStg+"\nS.C. Name : " + rs1.getString("HOA") +
                                "\nProcess Name : " + rs1.getString("PROCESS_NAME") +
                                "\nDC Number : " + rs1.getString("DC_NUMBER") +
                                "\nDC Date : " +  android.text.format.DateFormat.format("dd-MM-yyyy",rs1.getDate("DC_DATE")) +





                                "\nDC Out Quantity : " + rs1.getInt("OUTQTY") +
                                "\nDC In Quantity : " + rs1.getInt("INQTY") +
                                "\nStock : " + rs1.getInt("BALQTY");
                    }
                    rs1.close();
                    Log.d("SUBCONTRACT_FINAL", ScStg)

                    query1 = "SELECT HOA,PROCESS_NAME, QUANTITY FROM  PRPOUR_SUBCON AS P1" +
                            " LEFT OUTER JOIN ACHOA ON P1.SC_ID = ACHOA.ACCODE " +
                            " LEFT OUTER JOIN STPROCESS ON P1.PROCESS_ID = STPROCESS.PROCESS_ID " +
                            " WHERE ISNULL(P1.QUANTITY,0)>0 AND " +
                            " LTRIM(RTRIM(P1.HEAT_NO)) = '" + searchstring + "' " +
                            " AND P1.ITEM_ID = " + Titemid + " AND P1.GRADE_ID = " + Tgradeid +
                            " ORDER BY PROCESS_NAME,HOA ";
                    prepStmt2 = conn.prepareStatement(query1);
//prepStmt = conn.prepareStatement(query1);
// prepStmt.setString( 1, searchstring);
//Log.w("Query : :",prepStmt.toString() );
                    rs2 = prepStmt2.executeQuery();

                    Log.d("rs2", prepStmt2.toString())
                    while (rs2.next()) {




                        myresult = myresult + "\n" + rs2.getString("PROCESS_NAME").trim() +
                                " : " + rs2.getString("HOA")
                            .trim() + " - " + rs2.getInt("QUANTITY");
                        PourStg = PourStg + "\n" + rs2.getString("PROCESS_NAME").trim() +
                                " : " + rs2.getString("HOA")
                            .trim() + " - " + rs2.getInt("QUANTITY");
                    }
                    rs2.close();
                    query1 = "SELECT HOA,LOG_DATE, QUANTITY FROM  PRFETLOG_DETAILS AS P1" +
                            " LEFT OUTER JOIN PRFETLOG_HEADER AS P2 ON P1.LOG_NO = P2.LOG_NO AND P1.FIN_YEAR= P2.FIN_YEAR " +
                            " LEFT OUTER JOIN ACHOA ON P2.SC_ID = ACHOA.ACCODE " +
                            " WHERE ISNULL(P1.QUANTITY,0)>0 AND " +
                            " LTRIM(RTRIM(P1.HEAT_NO)) = '" + searchstring + "' " +
                            " AND P1.ITEM_ID = " + Titemid + " AND P1.GRADE_ID = " + Tgradeid +
                            " AND P1.WORDER_NO = '" + Tworderno + "' AND P1.WORDER_FIN_YEAR = '" + Tworderfinyear + "'" +
                            " AND P1.POSLNO = '" + Tposlno + "' ORDER BY LOG_DATE,HOA ";
                    prepStmt3 = conn.prepareStatement(query1);
//prepStmt = conn.prepareStatement(query1);
// prepStmt.setString( 1, searchstring);
//Log.w("Query : :",prepStmt.toString() );
                    rs3 = prepStmt3.executeQuery();

                    Log.d("rs3", prepStmt3.toString())
                    while (rs3.next()) {

                        Log.d("FETTLING", """
        SC Name: ${rs3.getString("HOA")}
        Date: ${rs3.getDate("LOG_DATE")}
        Qty: ${rs3.getInt("QUANTITY")}
    """.trimIndent())

                        myresult = myresult + "\n" + rs3.getString("HOA").trim() +
                                " : " + android.text.format.DateFormat.format(
                            "dd-MM-yyyy",
                            rs3.getDate("LOG_DATE")
                        ) + " - " + rs3.getInt("QUANTITY");
                        FettStg = FettStg + "\n" + rs3.getString("HOA").trim() +
                                " : " + android.text.format.DateFormat.format(
                            "dd-MM-yyyy",
                            rs3.getDate("LOG_DATE")
                        ) + " - " + rs3.getInt("QUANTITY");
                    }

                    Log.d("rs3",FettStg.toString())
                    rs3.close();
                }
                myresult = "Heat No.:" + searchstring + "\n" + ItemStg + "\n\nWork Order Details:\n" + WoStg + "\n\nPouring Details:\n" + PourStg + "\n\nProduction Details:\n" + PrdnStg;
                if (ScStg.length > 0) {
                    myresult = myresult + "\n\nSubcontract Details:\n" + ScStg;
                }
                if (FettStg.length > 0) {
                    myresult = myresult + "\n\nFettling Details:\n" + FettStg;
                }



                    val rsmd = rs.metaData



                // Organize into sections
                val sections = mutableListOf<DetailSection>().apply {
                    add(DetailSection(
                        title = "Work Order",
                        content = WoStg
                    ))
                    add(DetailSection(
                        title = "Pouring Details",
                        content = PourStg
                    ))
                    add(DetailSection(
                        title = "Production",
                        content = PrdnStg
                    ))
                    if (ScStg.isNotEmpty()) {
                        add(DetailSection(
                            title = "Subcontract",
                            content = ScStg
                        ))
                    }
                    if (FettStg.isNotEmpty()) {
                        add(DetailSection(
                            title = "Fettling",
                            content = FettStg
                        ))
                    }
                }

                Log.d("myresukt", myresult.toString())

                runOnUiThread {
                    hideLoadingDialog()
                    if (sections.isNotEmpty()) {
                        // Replace dummy values in summary
                        val updatedSummary = mainFields.map { field ->
                            when (field.label) {
                                "Casting Weight"      ->
                                    field.copy(value = details.find { it.label == "Casting Weight" }?.value ?: "N/A")
                                "Section Thickness"   ->
                                    field.copy(value = details.find { it.label == "Section Thickness" }?.value ?: "N/A")
                                else ->
                                    field
                            }
                        }


                        val combinedData = CombinedData(
                            mainData = updatedSummary,
                            detailedSections = sections
                        )

                        DetailDialogFragment.newInstance(combinedData)
                            .show(supportFragmentManager, "DetailDialog")
                    } else {
                        Toast.makeText(this, "No detailed data found", Toast.LENGTH_SHORT).show()
                    }
                }


            }catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    hideLoadingDialog()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                rs?.close()
                prepStmt?.close()
                conn?.close()
            }
        }.start()
    }

    private fun getConnection(): Connection? {
        val prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

        val server = prefs.getString("comp_server", "") ?: ""
        val port   = prefs.getString("comp_port", "1433") ?: "1433"
        val db     = prefs.getString("comp_db", "") ?: ""
        val user   = prefs.getString("comp_user", "") ?: ""
        val pass   = prefs.getString("comp_pass", "") ?: ""

        if (server.isBlank() || db.isBlank() || user.isBlank()) {
            runOnUiThread {
                Toast.makeText(this, "Database credentials not found.", Toast.LENGTH_LONG).show()
            }
            return null
        }

        return MSSQLConnection(server, port, db, user, pass).CONN()
    }


    private fun getFieldsForSection(allFields: List<DetailField>, labels: List<String>): List<DetailField> {
        return allFields.filter { it.label in labels }
    }

    // Helper extension function for creating sections
    private fun MutableList<DetailSection>.addSection(
        title: String,
        allFields: List<DetailField>,
        fieldLabels: List<String>
    ) {
        val sectionFields = allFields.filter { it.label in fieldLabels }
        if (sectionFields.isNotEmpty()) {
            this.add(DetailSection(title, sectionFields))
        }
    }
    private fun showLoadingDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setView(layoutInflater.inflate(R.layout.dialog_loading, null))
        builder.setCancelable(false)

        // create and force transparent window
        progressDialog = builder.create().apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            show()
        }
    }


    private fun hideLoadingDialog() {
        if (::progressDialog.isInitialized && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    class DetailAdapter(private val sections: List<DetailSection>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        companion object {
            private const val TYPE_HEADER = 0
            private const val TYPE_ITEM = 1
        }

        private val items = mutableListOf<Any>().apply {
            sections.forEach { section ->
                add(section.title)
                addAll(section.fields)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (items[position] is String) TYPE_HEADER else TYPE_ITEM
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                TYPE_HEADER -> SectionHeaderViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_section_header, parent, false)
                )
                else -> DetailItemViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_detail_row, parent, false)
                )
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is SectionHeaderViewHolder -> {
                    holder.bind(items[position] as String)
                }
                is DetailItemViewHolder -> {
                    holder.bind(items[position] as DetailField)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        class SectionHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvHeader: TextView = view.findViewById(R.id.tvSectionHeader)

            fun bind(title: String) {
                tvHeader.text = title
            }
        }

        class DetailItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvLabel: TextView = view.findViewById(R.id.tvLabel)
            private val tvValue: TextView = view.findViewById(R.id.tvValue)

            fun bind(field: DetailField) {
                tvLabel.text = field.label
                tvValue.text = field.value
            }
        }
    }}







