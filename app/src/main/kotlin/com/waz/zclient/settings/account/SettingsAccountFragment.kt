package com.waz.zclient.settings.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.waz.zclient.R
import com.waz.zclient.core.config.Config
import com.waz.zclient.core.extension.openUrl
import com.waz.zclient.settings.account.model.UserProfileItem
import kotlinx.android.synthetic.main.fragment_settings_account.*
import org.koin.android.viewmodel.ext.android.viewModel


class SettingsAccountFragment : Fragment() {

    private val settingsAccountViewModel: SettingsAccountViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar()
        initViewModel()
        setupListeners()
        loadData()
    }

    private fun initToolbar() {
        activity?.title = getString(R.string.pref_account_screen_title)
    }

    private fun initViewModel() {
        with(settingsAccountViewModel) {
            error.observe(viewLifecycleOwner) { errorMessage ->
                showErrorMessage(errorMessage)
            }
            profile.observe(viewLifecycleOwner) { profile ->
                updateProfile(profile)
            }

        }
    }

    private fun setupListeners() {
        preferences_account_reset_password.setOnClickListener { openUrl(getString(R.string.url_password_forgot).replaceFirst(Accounts, Config.accountsUrl())) }
    }

    private fun loadData() {
        lifecycleScope.launchWhenResumed {
            settingsAccountViewModel.loadData()
        }
    }

    private fun showErrorMessage(errorMessage: String) {
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun updateProfile(userProfileItem: UserProfileItem) {
        with(userProfileItem) {
            preferences_account_name_title.text = name
            preferences_account_handle_title.text = handle

            if (!email.isNullOrEmpty()) preferences_account_email_title.text = email
            else preferences_account_email_title.text = getString(R.string.pref_account_add_email_title)

            if (!phone.isNullOrEmpty()) preferences_account_phone_title.text = phone
            else preferences_account_phone_title.text = getString(R.string.pref_account_add_phone_title)
        }
    }

    companion object {
        fun newInstance() = SettingsAccountFragment()
        private const val Accounts = "|ACCOUNTS|"
    }


}


