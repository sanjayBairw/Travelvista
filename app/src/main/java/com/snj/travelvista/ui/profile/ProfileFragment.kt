package com.snj.travelvista.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.snj.travelvista.TravelApplication
import com.snj.travelvista.data.remote.provider.ProviderBookingLinkBuilder
import com.snj.travelvista.databinding.FragmentProfileBinding
import com.snj.travelvista.ui.common.Resource
import com.snj.travelvista.ui.common.ViewModelFactory
import com.snj.travelvista.utils.UserPreferences
import kotlinx.coroutines.launch
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val app: TravelApplication
        get() = requireActivity().application as TravelApplication

    private val viewModel: ProfileViewModel by viewModels {
        ViewModelFactory(app.repository, app.globalRepository)
    }

    private var converterFrom = "USD"
    private var converterTo = "EUR"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindPreferencesToUi()
        setupListeners()
        setupCurrencyConverter()
        observeViewModel()

        // Fetch fresh ECB rate for currently selected currency
        viewModel.refreshExchangeRate(app.userPreferences.preferredCurrency)
    }

    private fun bindPreferencesToUi() {
        val prefs = app.userPreferences
        binding.tvTravelerName.text = prefs.travelerName
        val homeSummary = buildString {
            if (prefs.homeCity.isNotBlank()) append(prefs.homeCity)
            if (prefs.homeAirport.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append("Airport: ${prefs.homeAirport}")
            }
        }.ifBlank { "Home City / Airport: Not set" }
        binding.tvTravelerHome.text = homeSummary

        binding.btnSelectCurrency.text = prefs.preferredCurrency
        binding.tvCurrencyRateInfo.text = if (prefs.preferredCurrency == "USD") {
            "USD · Base currency"
        } else {
            String.format(
                Locale.US,
                "1 USD ≈ %.3f %s (%s)",
                prefs.cachedUsdToPreferredRate,
                prefs.preferredCurrency,
                prefs.cachedRateDate.ifBlank { "ECB" }
            )
        }

        binding.switchMetricUnits.isChecked = prefs.useMetricUnits
        binding.tvUnitsSubtitle.text = if (prefs.useMetricUnits) "Metric (km, °C)" else "Imperial (mi, °F)"

        binding.tvThemeSubtitle.text = when (prefs.themeMode) {
            UserPreferences.THEME_LIGHT -> "Light Theme"
            UserPreferences.THEME_DARK -> "Dark Theme"
            else -> "System Default"
        }

        binding.tvPaymentSafety.text = ProviderBookingLinkBuilder.getPaymentSafetyNotice()
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        binding.btnSelectCurrency.setOnClickListener {
            val currencies = UserPreferences.SUPPORTED_CURRENCIES.toTypedArray()
            val currentIdx = currencies.indexOf(app.userPreferences.preferredCurrency).coerceAtLeast(0)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Preferred Currency (ECB Rates)")
                .setSingleChoiceItems(currencies, currentIdx) { dialog, which ->
                    val chosen = currencies[which]
                    app.userPreferences.preferredCurrency = chosen
                    binding.btnSelectCurrency.text = chosen
                    viewModel.refreshExchangeRate(chosen)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.switchMetricUnits.setOnCheckedChangeListener { _, isChecked ->
            app.userPreferences.useMetricUnits = isChecked
            binding.tvUnitsSubtitle.text = if (isChecked) "Metric (km, °C)" else "Imperial (mi, °F)"
        }

        binding.btnSelectTheme.setOnClickListener {
            val modes = arrayOf("System Default", "Light Theme", "Dark Theme")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("App Appearance")
                .setSingleChoiceItems(modes, app.userPreferences.themeMode) { dialog, which ->
                    app.userPreferences.themeMode = which
                    binding.tvThemeSubtitle.text = modes[which]
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.btnViewProviderStatus.setOnClickListener {
            showProviderStatusDialog()
        }
    }

    private fun setupCurrencyConverter() {
        converterTo = if (app.userPreferences.preferredCurrency != "USD") {
            app.userPreferences.preferredCurrency
        } else "EUR"

        binding.btnConverterFrom.text = "From: $converterFrom"
        binding.btnConverterTo.text = "To: $converterTo"

        val currencies = UserPreferences.SUPPORTED_CURRENCIES.toTypedArray()

        binding.btnConverterFrom.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Convert From")
                .setItems(currencies) { _, which ->
                    converterFrom = currencies[which]
                    binding.btnConverterFrom.text = "From: $converterFrom"
                    triggerConversion()
                }
                .show()
        }

        binding.btnConverterTo.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Convert To")
                .setItems(currencies) { _, which ->
                    converterTo = currencies[which]
                    binding.btnConverterTo.text = "To: $converterTo"
                    triggerConversion()
                }
                .show()
        }

        binding.btnConverterSwap.setOnClickListener {
            val temp = converterFrom
            converterFrom = converterTo
            converterTo = temp
            binding.btnConverterFrom.text = "From: $converterFrom"
            binding.btnConverterTo.text = "To: $converterTo"
            triggerConversion()
        }

        binding.btnExecuteConvert.setOnClickListener {
            triggerConversion()
        }

        triggerConversion()
    }

    private fun triggerConversion() {
        val amount = binding.etConverterAmount.text?.toString()?.toDoubleOrNull() ?: 100.0
        viewModel.calculateConversion(amount, converterFrom, converterTo)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currencyRate.collect { res ->
                        when (res) {
                            is Resource.Loading -> {
                                binding.tvCurrencyRateInfo.text = "Fetching live ECB rate..."
                            }
                            is Resource.Success -> {
                                val info = res.data
                                binding.tvCurrencyRateInfo.text = if (info.targetCurrency == "USD") {
                                    "USD · Base currency (Frankfurter ECB)"
                                } else {
                                    String.format(
                                        Locale.US,
                                        "1 USD = %.4f %s · ECB %s",
                                        info.rate,
                                        info.targetCurrency,
                                        info.date
                                    )
                                }
                            }
                            is Resource.Error -> {
                                binding.tvCurrencyRateInfo.text =
                                    "${app.userPreferences.preferredCurrency} · Using cached rate (Offline)"
                            }
                            null -> Unit
                        }
                    }
                }

                launch {
                    viewModel.conversionResult.collect { res ->
                        when (res) {
                            is Resource.Loading -> {
                                binding.tvConversionOutput.text = "Calculating live ECB exchange..."
                            }
                            is Resource.Success -> {
                                val r = res.data
                                binding.tvConversionOutput.text = String.format(
                                    Locale.US,
                                    "%.2f %s = %.2f %s",
                                    r.fromAmount,
                                    r.fromCurrency,
                                    r.toAmount,
                                    r.toCurrency
                                )
                                binding.tvConversionRateDetail.text = String.format(
                                    Locale.US,
                                    "1 %s = %.4f %s · %s (%s)",
                                    r.fromCurrency,
                                    r.rate,
                                    r.toCurrency,
                                    r.sourceAttribution,
                                    r.date
                                )
                            }
                            is Resource.Error -> {
                                binding.tvConversionOutput.text = "Conversion result unavailable"
                                binding.tvConversionRateDetail.text = res.message
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun showEditProfileDialog() {
        val ctx = requireContext()
        val pad = (16 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, pad / 2)
        }
        val prefs = app.userPreferences
        val etName = EditText(ctx).apply {
            hint = "Traveler Name"
            setText(prefs.travelerName)
        }
        val etCity = EditText(ctx).apply {
            hint = "Home City (e.g. Mumbai, London)"
            setText(prefs.homeCity)
        }
        val etAirport = EditText(ctx).apply {
            hint = "Home Airport IATA (e.g. BOM, LHR, JFK)"
            setText(prefs.homeAirport)
        }
        container.addView(etName)
        container.addView(etCity)
        container.addView(etAirport)

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Edit Local Traveler Profile")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                prefs.travelerName = etName.text.toString()
                prefs.homeCity = etCity.text.toString()
                prefs.homeAirport = etAirport.text.toString()
                bindPreferencesToUi()
                Snackbar.make(binding.root, "Profile updated on device", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showProviderStatusDialog() {
        val message = ProviderBookingLinkBuilder.capabilities.joinToString("\n\n") { cap ->
            "• ${cap.serviceName} [${cap.mode.label}]\n  Provider: ${cap.providerName}\n  ${cap.description}\n  Credentials: ${cap.requiredCredentials}"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("TravelVista Global API & Booking Status")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
