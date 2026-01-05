package com.example.moneytracker

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // ---------- TOGGLE BUTTONS ----------
    private lateinit var btnFlowDebit: MaterialButton
    private lateinit var btnFlowCredit: MaterialButton
    private lateinit var btnDebitAccount: MaterialButton
    private lateinit var btnCreditAccount: MaterialButton

    // ---------- INPUT ----------
    private lateinit var etDate: EditText
    private lateinit var etAmount: EditText
    private lateinit var etReason: EditText

    // ---------- DROPDOWNS ----------
    private lateinit var dropdownMethod: MaterialAutoCompleteTextView
    private lateinit var dropdownBank: MaterialAutoCompleteTextView
    private lateinit var dropdownCategory: MaterialAutoCompleteTextView

    // ---------- MONTHLY BUDGET ----------
    private lateinit var tilMonthlyBudget: TextInputLayout
    private lateinit var etMonthlyBudget: TextInputEditText
    private lateinit var layoutMonthlyBudgetDisplay: LinearLayout
    private lateinit var tvMonthlyBudgetDisplay: TextView
    private lateinit var ivBudgetAction: ImageView
    private lateinit var tvRemaining: TextView

    private var isDebitSelected = false
    private var isCreditSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadMonthlyBudget()
        setupDropdowns()
        setupDatePicker()
        setupToggleButtons()
        setupSaveButton()
        setupClearButton()
    }

    // ---------- INIT ----------
    private fun initViews() {
        btnFlowDebit = findViewById(R.id.btnFlowDebit)
        btnFlowCredit = findViewById(R.id.btnFlowCredit)
        btnDebitAccount = findViewById(R.id.btnDebitAccount)
        btnCreditAccount = findViewById(R.id.btnCreditAccount)

        etDate = findViewById(R.id.etDate)
        etAmount = findViewById(R.id.etAmount)
        etReason = findViewById(R.id.etReason)

        dropdownMethod = findViewById(R.id.dropdownMethod)
        dropdownBank = findViewById(R.id.dropdownBank)
        dropdownCategory = findViewById(R.id.dropdownCategory)

        tilMonthlyBudget = findViewById(R.id.tilMonthlyBudget)
        etMonthlyBudget = findViewById(R.id.etMonthlyBudget)

        layoutMonthlyBudgetDisplay = findViewById(R.id.layoutMonthlyBudgetDisplay)
        tvMonthlyBudgetDisplay = findViewById(R.id.tvMonthlyBudgetDisplay)
        ivBudgetAction = findViewById(R.id.ivBudgetAction)
        tvRemaining = findViewById(R.id.tvRemaining)
    }

    // ---------- MONTHLY BUDGET ----------
    private fun loadMonthlyBudget() {
        val prefs = getSharedPreferences("budget_prefs", MODE_PRIVATE)

        val budget = prefs.getString("budget_amount", null)
        val year = prefs.getInt("budget_year", -1)
        val month = prefs.getInt("budget_month", -1)
        val day = prefs.getInt("budget_day", -1)

        if (budget.isNullOrEmpty()) {
            showBudgetInput()
            tvRemaining.text = "Remaining: ₹ 0"
            return
        }

        val today = Calendar.getInstance()
        val saved = Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
        }

        val diffDays =
            ((today.timeInMillis - saved.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

        val editable =
            today.get(Calendar.MONTH) == month &&
                    today.get(Calendar.YEAR) == year &&
                    diffDays <= 2

        showBudgetText(budget, editable)

        val remaining = prefs.getInt("remaining_balance", budget.toInt())
        updateRemainingUI(remaining)
    }

    private fun showBudgetInput() {
        tilMonthlyBudget.visibility = View.VISIBLE
        layoutMonthlyBudgetDisplay.visibility = View.GONE
    }

    private fun showBudgetText(amount: String, editable: Boolean) {
        tilMonthlyBudget.visibility = View.GONE
        layoutMonthlyBudgetDisplay.visibility = View.VISIBLE

        tvMonthlyBudgetDisplay.text = "This Month Budget : ₹ $amount"

        if (editable) {
            ivBudgetAction.setImageResource(R.drawable.ic_edit)
            ivBudgetAction.isEnabled = true
            ivBudgetAction.setOnClickListener {
                showBudgetInput()
                etMonthlyBudget.setText(amount)
                etMonthlyBudget.requestFocus()
            }
        } else {
            ivBudgetAction.setImageResource(R.drawable.ic_lock)
            ivBudgetAction.isEnabled = false
            ivBudgetAction.setOnClickListener(null)
        }
    }

    private fun updateRemainingUI(value: Int) {
        tvRemaining.text = "Remaining: ₹ $value"
    }

    // ---------- SAVE ----------
    private fun setupSaveButton() {
        findViewById<Button>(R.id.btnSave).setOnClickListener {

            val prefs = getSharedPreferences("budget_prefs", MODE_PRIVATE)

            // ---------- SAVE BUDGET ----------
            val budgetText = etMonthlyBudget.text?.toString()?.trim()
            if (!budgetText.isNullOrEmpty()) {

                val budget = budgetText.toInt()
                val now = Calendar.getInstance()

                prefs.edit()
                    .putString("budget_amount", budgetText)
                    .putInt("budget_year", now.get(Calendar.YEAR))
                    .putInt("budget_month", now.get(Calendar.MONTH))
                    .putInt("budget_day", now.get(Calendar.DAY_OF_MONTH))
                    .putInt("remaining_balance", budget) // 🔥 KEY FIX
                    .apply()

                showBudgetText(budgetText, editable = true)
                updateRemainingUI(budget)

                etMonthlyBudget.text?.clear()

                Toast.makeText(this, "Monthly budget saved", Toast.LENGTH_SHORT).show()
                return@setOnClickListener   // ⛔ STOP here (VERY IMPORTANT)
            }

            // ---------- TRANSACTION ----------
            val amountText = etAmount.text.toString().trim()
            if (amountText.isEmpty()) {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toInt()
            val currentRemaining = prefs.getInt("remaining_balance", 0)

            val newRemaining = when {
                isDebitSelected -> currentRemaining - amount
                isCreditSelected -> currentRemaining + amount
                else -> {
                    Toast.makeText(this, "Select Debit or Credit", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            prefs.edit()
                .putInt("remaining_balance", newRemaining)
                .apply()

            updateRemainingUI(newRemaining)

            etAmount.text.clear()
            etReason.text.clear()

            Toast.makeText(this, "Transaction saved", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- DROPDOWNS ----------
    private fun setupDropdowns() {
        setupDropdown(dropdownMethod, listOf("CASH", "G-PAY", "CRED", "NAVI", "NET-BANKING"))
        setupDropdown(dropdownBank, listOf("HDFC", "HDFC CREDIT", "FEDERAL BANK", "SBI", "ICICI"))
        setupDropdown(dropdownCategory, listOf("General", "Investment", "Savings"))
    }

    private fun setupDropdown(dropdown: MaterialAutoCompleteTextView, items: List<String>) {
        dropdown.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        )
    }

    // ---------- DATE ----------
    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        etDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    calendar.set(y, m, d)
                    etDate.setText(formatter.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    // ---------- TOGGLES ----------
    private fun setupToggleButtons() {
        btnFlowDebit.setOnClickListener {
            select(btnFlowDebit); unselect(btnFlowCredit)
            isDebitSelected = true
            isCreditSelected = false
        }

        btnFlowCredit.setOnClickListener {
            select(btnFlowCredit); unselect(btnFlowDebit)
            isDebitSelected = false
            isCreditSelected = true
        }

        btnDebitAccount.setOnClickListener {
            select(btnDebitAccount); unselect(btnCreditAccount)
        }

        btnCreditAccount.setOnClickListener {
            select(btnCreditAccount); unselect(btnDebitAccount)
        }
    }

    private fun select(btn: MaterialButton) {
        btn.setBackgroundColor(Color.parseColor("#7E57C2"))
        btn.setTextColor(Color.WHITE)
    }

    private fun unselect(btn: MaterialButton) {
        btn.setBackgroundColor(Color.parseColor("#444444"))
        btn.setTextColor(Color.parseColor("#AAAAAA"))
    }

    // ---------- CLEAR ----------
    private fun setupClearButton() {
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            etDate.text.clear()
            etAmount.text.clear()
            etReason.text.clear()

            dropdownMethod.setText("", false)
            dropdownBank.setText("", false)
            dropdownCategory.setText("", false)

            unselect(btnFlowDebit)
            unselect(btnFlowCredit)
            unselect(btnDebitAccount)
            unselect(btnCreditAccount)

            isDebitSelected = false
            isCreditSelected = false
        }
    }
}