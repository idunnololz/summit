package com.idunnololz.summit.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idunnololz.summit.scrape.WebsiteAdapterLoader
import com.idunnololz.summit.R
import com.idunnololz.summit.api.ApiException
import com.idunnololz.summit.api.ClientApiException
import com.idunnololz.summit.api.NetworkException
import com.idunnololz.summit.api.NoInternetException
import com.idunnololz.summit.api.NotAuthenticatedException
import com.idunnololz.summit.api.ServerApiException
import com.idunnololz.summit.api.ServerTimeoutException
import com.idunnololz.summit.api.SocketTimeoutException
import com.idunnololz.summit.scrape.LoaderException
import com.idunnololz.summit.util.AnimationUtils
import com.idunnololz.summit.util.toErrorMessage

class LoadingView : ConstraintLayout {

    companion object {
        private const val TAG = "LoadingView"
    }

    private lateinit var container: View
    private lateinit var errorTextView: TextView
    private lateinit var positiveButton: Button
    private lateinit var progressView: View
    private lateinit var negativeButton: Button
    private val rootViewAnimationController: AnimationUtils.AnimationController
    private var onRefreshClickListener: (View) -> Unit = {}

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        View.inflate(context, R.layout.refresh_ui, this)
        rootViewAnimationController = AnimationUtils.makeAnimationControllerFor(this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        container = this
        errorTextView = rootView.findViewById(R.id._error)
        positiveButton = rootView.findViewById(R.id._positive_button)
        progressView = rootView.findViewById(R.id._progress_view)
        negativeButton = rootView.findViewById(R.id._negative_button)
    }

    private fun ensureRootVisible() {
        rootViewAnimationController.show(false)
    }

    private fun show(
        progressBar: Boolean = false,
        errorText: Boolean = false,
        positiveButton: Boolean = false,
        negativeButton: Boolean = false
    ) {
        //val animateChanges = rootViewAnimationController.isVisible

        rootViewAnimationController.cancelAnimations()
        progressView.visibility = if (progressBar) View.VISIBLE else View.GONE
        errorTextView.visibility = if (errorText) View.VISIBLE else View.GONE
        this.positiveButton.visibility = if (positiveButton) View.VISIBLE else View.GONE
        this.negativeButton.visibility = if (negativeButton) View.VISIBLE else View.GONE
        ensureRootVisible()
    }

    private fun bindErrorText(string: String) {
        errorTextView.text = string
    }

    private fun bindPositiveButton(text: String, listener: (View) -> Unit) {
        positiveButton.text = text
        positiveButton.setOnClickListener(listener)
    }

    private fun bindNegativeButton(text: String, listener: (View) -> Unit) {
        negativeButton.text = text
        negativeButton.setOnClickListener(listener)
    }

    fun setOnRefreshClickListener(l: (View) -> Unit) {
        onRefreshClickListener = l
    }

    /**
     * Shows the progress bar. Calling this will hide all other shared ui elements such as errors.
     */
    fun showProgressBar() {
        show(progressBar = true)
    }

    fun showProgressBarWithMessage(@StringRes message: Int) {
        show(progressBar = true, errorText = true)
        bindErrorText(context.getString(message))
        progressView.visibility = View.VISIBLE
    }

    fun showProgressBarWithMessageAndButton(
        @StringRes message: Int,
        @StringRes buttonText: Int,
        onClickListener: (View) -> Unit
    ) {
        show(progressBar = true, errorText = true, positiveButton = true)
        bindErrorText(context.getString(message))
        bindPositiveButton(context.getString(buttonText), onClickListener)
        progressView.visibility = View.VISIBLE
    }

    fun showErrorText(strId: Int) {
        showErrorText(context.getString(strId))
    }

    fun showErrorText(string: String) {
        show(errorText = true)
        bindErrorText(string)
    }

    fun showErrorWithButton(errorText: String, buttonText: String, listener: (View) -> Unit) {
        show(errorText = true, positiveButton = true)
        bindErrorText(errorText)
        bindPositiveButton(buttonText, listener)
    }

    fun showErrorWithTwoButtons(
        errorText: String,
        positiveButtonText: String,
        positiveButtonListener: (View) -> Unit,
        negativeButtonText: String,
        negativeButtonListener: (View) -> Unit
    ) {
        show(errorText = true, positiveButton = true, negativeButton = true)
        bindErrorText(errorText)
        bindPositiveButton(positiveButtonText, positiveButtonListener)
        bindNegativeButton(negativeButtonText, negativeButtonListener)
    }

    fun showErrorWithRetry(msg: Int) {
        showErrorWithRetry(context.getString(msg))
    }

    fun showErrorWithRetry(msg: String, retryText: String = context.getString(R.string.retry)) {
        show(errorText = true, positiveButton = true)
        bindErrorText(msg)
        bindPositiveButton(retryText) { v ->
            showProgressBar()
            onRefreshClickListener(v)
        }
    }

    fun showErrorWithRetry(
        @StringRes msg: Int,
        @StringRes negativeButtonText: Int,
        negativeButtonListener: (View) -> Unit
    ) {
        show(errorText = true, positiveButton = true, negativeButton = true)
        bindErrorText(context.getString(msg))
        bindPositiveButton(context.getString(R.string.retry)) { v ->
            showProgressBar()
            onRefreshClickListener(v)
        }
        bindNegativeButton(context.getString(negativeButtonText), negativeButtonListener)
    }

    fun showDefaultErrorMessageFor(errorCode: Int) {
        showErrorWithRetry(WebsiteAdapterLoader.getDefaultErrorMessageFor(errorCode))
    }

    fun showDefaultErrorMessageFor(t: Throwable) {
        showErrorWithRetry(t.toErrorMessage(context))
    }

    fun hideAll(animate: Boolean = true, makeViewGone: Boolean = false) {
        rootViewAnimationController.hideVisibility = if (makeViewGone) View.GONE else View.INVISIBLE
        rootViewAnimationController.hide(animate)
    }
}