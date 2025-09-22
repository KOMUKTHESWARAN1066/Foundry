package com.MaDuSOFTSolutions.foundry

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.sql.ResultSet

class LoginActivity : AppCompatActivity() {

    data class Company(
        val compName: String,
        val dbName: String = "",
        val server: String = "",
        val user: String = "",
        val pass: String = ""
    ) {
        override fun toString(): String = compName
    }

    private lateinit var spinner: Spinner
    private lateinit var btnLogin: Button
    private lateinit var edtUsername: EditText
    private lateinit var edtPassword: EditText
    private var selectedCompany: Company? = null
    private val dbPort = "1433"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        spinner = findViewById(R.id.spinnerCompany)
        btnLogin = findViewById(R.id.btnLogin)
        edtUsername = findViewById(R.id.editUsername)
        edtPassword = findViewById(R.id.editPassword)

        loadCompanyNames()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val compName = parent.getItemAtPosition(position).toString()
                fetchCompanyDetails(compName)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val showPasswordCheckBox = findViewById<CheckBox>(R.id.checkboxShowPassword)

        showPasswordCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                edtPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                edtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            edtPassword.setSelection(edtPassword.text.length)
        }



        btnLogin.setOnClickListener {
            authenticateUser()
        }
    }

    /** Step 1: Load only company names into the spinner */
    private fun loadCompanyNames() {
        Thread {
            val names = mutableListOf<String>()
            val conn = MSSQLConnection("103.38.50.149", "5121", "FOUNDRY", "tasksa", "Task@Sa").CONN()
            if (conn != null) {
                try {
                    val stmt = conn.prepareStatement("SELECT COMPNAME FROM DBLIST")
                    val rs: ResultSet = stmt.executeQuery()
                    while (rs.next()) {
                        names.add(rs.getString("COMPNAME"))
                    }
                    rs.close()
                    stmt.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    conn.close()
                }
            }

            runOnUiThread {
                if (names.isNotEmpty()) {
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                } else {
                    Toast.makeText(this, "No companies found.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /** Step 2: Fetch connection details after selecting a company */
    private fun fetchCompanyDetails(compName: String) {
        Thread {
            val conn = MSSQLConnection("103.38.50.149", "5121", "FOUNDRY", "tasksa", "Task@Sa").CONN()
            if (conn != null) {
                try {
                    val stmt = conn.prepareStatement(
                        "SELECT COMPNAME, DBNAME, SERVERNAME, USERNAME, PASSWORD FROM DBLIST WHERE COMPNAME = ?"
                    )
                    stmt.setString(1, compName)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        selectedCompany = Company(
                            rs.getString("COMPNAME"),
                            rs.getString("DBNAME"),
                            rs.getString("SERVERNAME"),
                            rs.getString("USERNAME"),
                            rs.getString("PASSWORD")
                        )
                    }
                    rs.close()
                    stmt.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    conn.close()
                }
            }
        }.start()
    }

    /** Step 3: Authenticate using selected company's DB connection */
    private fun authenticateUser() {
        val userInput = edtUsername.text.toString()
        val passInput = edtPassword.text.toString()
        val company = selectedCompany

        if (company == null) {
            Toast.makeText(this, "Please select a company.", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            val (server, port) = if (company.server.contains(",")) {
                company.server.split(",").map { it.trim() }
            } else {
                listOf(company.server, dbPort)
            }

            val conn = MSSQLConnection(
                server,
                port,
                company.dbName,
                company.user,
                company.pass
            ).CONN()
            if (conn != null) {
                try {
                    val stmt = conn.prepareStatement(
                        "SELECT * FROM STUSERDEPT WHERE NICKNAME=? AND PASSWORD=?"
                    )
                    stmt.setString(1, userInput)
                    stmt.setString(2, passInput)

                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        val KUSERNAME = rs.getString("USERNAME")
                        Log.d("username", KUSERNAME)

                        // Check view rights for 'NMINFORM'
                        val stmt2 = conn.prepareStatement(
                            "SELECT * FROM STUSERFORMS WHERE FORMNAME = 'NMINFORM' AND [USERNAME] = ? AND [VIEW] = 1"
                        )
                        stmt2.setString(1, KUSERNAME)
                        val rs2 = stmt2.executeQuery()
                        Log.d("query",rs2.toString())

                        Log.d("query result",rs2.metaData.toString())
                        val canViewPurchase = rs2.next()

                        rs2.close()
                        stmt2.close()

                        // Save login & permission info
                        val sharedPref = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                        sharedPref.edit().apply {
                            putBoolean("canViewPurchase", canViewPurchase)
                            putString("comp_server", server)
                            putString("comp_port", port)
                            putString("comp_db", company.dbName)
                            putString("comp_user", company.user)
                            putString("comp_pass", company.pass)
                            putBoolean("isLoggedIn", true)
                            putBoolean("exitApp", false)
                            apply()
                        }

                        runOnUiThread {
                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                            startService(Intent(this@LoginActivity, LogoutService::class.java))
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                        }
                    }

                    rs.close()
                    stmt.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this, "Login error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    conn.close()
                }

            } else {
                runOnUiThread {
                    Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
