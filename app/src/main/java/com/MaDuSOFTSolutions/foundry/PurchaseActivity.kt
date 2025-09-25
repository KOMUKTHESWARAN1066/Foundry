package com.MaDuSOFTSolutions.foundry

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PurchaseActivity : AppCompatActivity() {

    private lateinit var itemSpinner: Spinner
    private lateinit var itemSearchEditText: EditText
    private lateinit var fromDateEditText: EditText
    private lateinit var toDateEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var fetchButton: Button
    private val itemMap = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase)

        itemSpinner = findViewById(R.id.itemSpinner)
        itemSearchEditText = findViewById(R.id.editTextText)
        fromDateEditText = findViewById(R.id.editTextFromDate)
        toDateEditText = findViewById(R.id.editTextToDate)
        searchButton = findViewById(R.id.searchButton)
        fetchButton = findViewById(R.id.fetchDetailsButton)

        fromDateEditText.setOnClickListener { showDatePickerDialog(fromDateEditText) }
        toDateEditText.setOnClickListener { showDatePickerDialog(toDateEditText) }

        searchButton.setOnClickListener {
            val searchTerm = itemSearchEditText.text.toString().trim()
            if (searchTerm.isEmpty()) {
                Toast.makeText(this, "Enter item name to search", Toast.LENGTH_SHORT).show()
            } else {
                FetchItemsTask().execute(searchTerm)
            }
        }

        fetchButton.setOnClickListener {
            val selectedItem = itemSpinner.selectedItem?.toString()
            val fromDateStr = fromDateEditText.text.toString()
            val toDateStr = toDateEditText.text.toString()

            if (selectedItem.isNullOrBlank() || fromDateStr.isBlank() || toDateStr.isBlank()) {
                Toast.makeText(this, "Please select item and dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val itemId = itemMap[selectedItem]
            if (itemId != null) {
                FetchPurchaseDetailsTask(itemId, fromDateStr, toDateStr, selectedItem).execute()

            }
        }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = sdf.format(calendar.time)
            editText.setText(formattedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    inner class FetchItemsTask : AsyncTask<String, Void, List<String>>() {
        override fun doInBackground(vararg params: String): List<String> {
            val itemList = mutableListOf<String>()
            itemMap.clear()
            val searchInput = params.firstOrNull()?.trim() ?: ""
            var conn: Connection? = null
            try {
                conn = getConnection()
                val stmt =
                    conn?.prepareStatement("SELECT ITEM_ID, ITEM_NAME FROM STITEM WHERE ITEM_NAME LIKE ? ORDER BY ITEM_NAME")
                stmt?.setString(1, "%$searchInput%")
                val rs = stmt?.executeQuery()
                while (rs != null && rs.next()) {
                    val id = rs.getString("ITEM_ID")
                    val name = rs.getString("ITEM_NAME")
                    itemList.add(name)
                    itemMap[name] = id
                }
                rs?.close()
                stmt?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn?.close()
            }
            return itemList
        }


        override fun onPostExecute(result: List<String>) {
            val adapter = ArrayAdapter(this@PurchaseActivity, android.R.layout.simple_spinner_dropdown_item, result)
            itemSpinner.adapter = adapter
            if (result.isEmpty()) {
                Toast.makeText(this@PurchaseActivity, "No items found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class FetchPurchaseDetailsTask(
        private val itemId: String,
        private val fromDate: String,
        private val toDate: String,
        private val selectedItemName: String
    ) : AsyncTask<Void, Void, List<PurchaseRecord>>() {


        override fun doInBackground(vararg params: Void?): List<PurchaseRecord> {
            val list = mutableListOf<PurchaseRecord>()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val fromSql = sdf.format(sdf.parse(fromDate)!!)
            val toSql = sdf.format(sdf.parse(toDate)!!)
            var conn: Connection? = null

            try {
                conn = getConnection()

                val query = """
                    SELECT P1.MIN_NO, P2.MIN_DATE, HOA, P2.BILL_NO, P2.BILL_DATE, P1.ACCQTY, P1.RATE, P1.AMOUNT, S1.ITEM_NAME 
                    FROM STMI_DETAILS AS P1 
                    LEFT OUTER JOIN STMI_HEADER AS P2 ON P1.MIN_NO = P2.MIN_NO AND P1.FIN_YEAR = P2.FIN_YEAR 
                    LEFT OUTER JOIN STITEM AS S1 ON P1.ITEM_ID = S1.ITEM_ID 
                    LEFT OUTER JOIN ACHOA AS A1 ON P2.PARTY_ID = A1.ACCODE 
                    WHERE P1.ITEM_ID = $itemId
                      AND P2.MIN_DATE >= '$fromDate'
                      AND P2.MIN_DATE <= '$toDate'
                    ORDER BY P2.MIN_DATE DESC,P1.MIN_NO
                """.trimIndent()

                val stmt = conn!!.prepareStatement(query)


                val rs = stmt.executeQuery()

                while (rs.next()) {
                    // Input format: SQL Server typically returns "yyyy-MM-dd HH:mm:ss" or similar
                    val rawMinDate = rs.getString("MIN_DATE")
                    val rawBillDate = rs.getString("BILL_DATE")

// Parse the raw dates
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val displayFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)

                    val formattedMinDate = try {
                        val parsedDate = inputFormat.parse(rawMinDate)
                        displayFormat.format(parsedDate!!)
                    } catch (e: Exception) {
                        rawMinDate // fallback
                    }

                    val formattedBillDate = try {
                        val parsedDate = inputFormat.parse(rawBillDate)
                        displayFormat.format(parsedDate!!)
                    } catch (e: Exception) {
                        rawBillDate // fallback
                    }

                    val record = PurchaseRecord(
                        minNo = rs.getString("MIN_NO"),
                        minDate = formattedMinDate,
                        hoa = rs.getString("HOA"),
                        billNo = rs.getString("BILL_NO"),
                        billDate = formattedBillDate,
                        qty = rs.getDouble("ACCQTY"),
                        rate = rs.getDouble("RATE"),
                        amount = rs.getDouble("AMOUNT")
                    )

                    list.add(record)
                }
                Log.d("record", list.toString())
                rs.close()
                stmt.close()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn?.close()
            }

            return list
        }

        override fun onPostExecute(result: List<PurchaseRecord>) {
            val dialogView = layoutInflater.inflate(R.layout.combined_purchase_layout, null)
            val gridLayout = dialogView.findViewById<GridLayout>(R.id.gridLayout)
            val titleView = layoutInflater.inflate(R.layout.custom_dialog_title, null)
            val titleMain = titleView.findViewById<TextView>(R.id.titleMain)
            val titleSub = titleView.findViewById<TextView>(R.id.titleSub)

            titleMain.text = "Purchase Records"
            titleSub.text = "Item Name - $selectedItemName"


            val headers =
                listOf("MIN No", "Min Date", "Supplier Name", "Bill No", "Bill Date", "Qty", "Rate", "Amount")

            for (i in headers.indices) {
                gridLayout.addView(TextView(this@PurchaseActivity).apply {
                    text = " "
                    height = 24// or use setPadding() if you prefer spacing that way
                })
            }

            // Add header row
            for (header in headers) {
                gridLayout.addView(makeCell(header, isHeader = true))
            }

// Add an empty row as spacer


            // Add data rows
            for (record in result) {
                gridLayout.addView(makeCell(record.minNo))
                gridLayout.addView(makeCell(record.minDate))
                gridLayout.addView(makeCell(record.hoa))
                gridLayout.addView(makeCell(record.billNo))
                gridLayout.addView(makeCell(record.billDate))
                gridLayout.addView(makeCell("%.2f".format(record.qty)))
                gridLayout.addView(makeCell("%.2f".format(record.rate)))
                gridLayout.addView(makeCell("%.2f".format(record.amount)))
            }

            // Add vertical space below data rows
            for (i in headers.indices) {
                gridLayout.addView(TextView(this@PurchaseActivity).apply {
                    text = ""
                    height = 24 // Adjust the spacing as needed
                })
            }

            AlertDialog.Builder(this@PurchaseActivity)
                .setCustomTitle(titleView)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show()

        }
    }

    private fun makeCell(text: String, isHeader: Boolean = false): TextView {
        return TextView(this@PurchaseActivity).apply {
            setText(text)

            setBackgroundResource(R.drawable.cell_background)
            textSize = 14f

            if (isHeader) {
                setTypeface(null, android.graphics.Typeface.BOLD)
                setBackgroundColor(android.graphics.Color.parseColor("#D3D3D3")) // Light gray for header
            }

            // Optional: Add borders or background to make it look like a table
            setBackgroundResource(android.R.drawable.editbox_background) // Or use a custom drawable for fine control
        }
    }



    private fun getConnection(): Connection? {
        val prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val server = prefs.getString("comp_server", "") ?: ""
        val port = prefs.getString("comp_port", "1433") ?: "1433"
        val db = prefs.getString("comp_db", "") ?: ""
        val user = prefs.getString("comp_user", "") ?: ""
        val pass = prefs.getString("comp_pass", "") ?: ""

        if (server.isBlank() || db.isBlank() || user.isBlank()) {
            runOnUiThread {
                Toast.makeText(this, "Database credentials not found", Toast.LENGTH_LONG).show()
            }
            return null
        }

        return MSSQLConnection(server, port, db, user, pass).CONN()
    }
}
