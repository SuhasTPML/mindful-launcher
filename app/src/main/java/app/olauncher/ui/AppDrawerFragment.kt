package app.olauncher.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentAppDrawerBinding
import app.olauncher.helper.hideKeyboard
import app.olauncher.helper.isEinkDisplay
import app.olauncher.helper.isSystemApp
import app.olauncher.helper.openAppInfo
import app.olauncher.helper.openSearch
import app.olauncher.helper.openUrl
import app.olauncher.helper.showKeyboard
import app.olauncher.helper.showToast
import app.olauncher.helper.uninstall
import androidx.appcompat.app.AlertDialog
import java.text.Normalizer


class AppDrawerFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var adapter: AppDrawerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    private var flag = Constants.FLAG_LAUNCH_APP
    private var canRename = false

    // A–Z fast scroll index support
    private val letters: List<Char> = ('A'..'Z').toList()
    private val letterPositions: MutableMap<Char, Int> = mutableMapOf()
    private val overlayHideHandler = Handler(Looper.getMainLooper())
    private val overlayHideRunnable = Runnable { binding.letterOverlay.visibility = View.GONE }

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        arguments?.let {
            flag = it.getInt(Constants.Key.FLAG, Constants.FLAG_LAUNCH_APP)
            canRename = it.getBoolean(Constants.Key.RENAME, false)
        }
        initViews()
        initSearch()
        initAdapter()
        initObservers()
        initClickListeners()
        initLetterIndex()
    }

    private fun initViews() {
        if (flag == Constants.FLAG_HIDDEN_APPS)
            binding.search.queryHint = getString(R.string.hidden_apps)
        else if (flag in Constants.FLAG_SET_HOME_APP_1..Constants.FLAG_SET_CALENDAR_APP)
            binding.search.queryHint = "Please select an app"
        try {
            val searchTextView = binding.search.findViewById<TextView>(R.id.search_src_text)
            if (searchTextView != null) searchTextView.gravity = prefs.appLabelAlignment
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initSearch() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query?.startsWith("!") == true)
                    requireContext().openUrl(Constants.URL_DUCK_SEARCH + query.replace(" ", "%20"))
                else if (adapter.itemCount == 0) // && requireContext().searchOnPlayStore(query?.trim()).not())
                    requireContext().openSearch(query?.trim())
                else
                    adapter.launchFirstInList()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                try {
                    adapter.filter.filter(newText)
                    binding.appDrawerTip.visibility = View.GONE
                    binding.appRename.visibility = if (canRename && newText.isNotBlank()) View.VISIBLE else View.GONE
                    binding.recyclerView.post { rebuildLetterPositions() }
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
        })
    }

    private fun initAdapter() {
        adapter = AppDrawerAdapter(
            flag,
            prefs.appLabelAlignment,
            appClickListener = {
                if (it.appPackage.isEmpty())
                    return@AppDrawerAdapter
                if (flag == Constants.FLAG_SELECT_DELAY_APP) {
                    val set = prefs.mindfulDelayedApps.toMutableSet()
                    set.add(it.appPackage)
                    prefs.mindfulDelayedApps = set
                    // Prompt for per-app delay seconds
                    showDelayStepper(it.appPackage)
                    findNavController().popBackStack()
                } else {
                    viewModel.selectedApp(it, flag)
                    if (flag == Constants.FLAG_LAUNCH_APP || flag == Constants.FLAG_HIDDEN_APPS)
                        findNavController().popBackStack(R.id.mainFragment, false)
                    else
                        findNavController().popBackStack()
                }
            },
            appInfoListener = {
                openAppInfo(
                    requireContext(),
                    it.user,
                    it.appPackage
                )
                findNavController().popBackStack(R.id.mainFragment, false)
            },
            appDeleteListener = {
                requireContext().apply {
                    if (isSystemApp(it.appPackage))
                        showToast(getString(R.string.system_app_cannot_delete))
                    else
                        uninstall(it.appPackage)
                }
            },
            appHideListener = { appModel, position ->
                adapter.appFilteredList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.appsList.remove(appModel)

                val newSet = mutableSetOf<String>()
                newSet.addAll(prefs.hiddenApps)
                if (flag == Constants.FLAG_HIDDEN_APPS) {
                    newSet.remove(appModel.appPackage) // for backward compatibility
                    newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
                } else
                    newSet.add(appModel.appPackage + "|" + appModel.user.toString())

                prefs.hiddenApps = newSet
                if (newSet.isEmpty())
                    findNavController().popBackStack()
                if (prefs.firstHide) {
                    binding.search.hideKeyboard()
                    prefs.firstHide = false
                    viewModel.showDialog.postValue(Constants.Dialog.HIDDEN)
                    findNavController().navigate(R.id.action_appListFragment_to_settingsFragment2)
                }
                viewModel.getAppList()
                viewModel.getHiddenApps()
            },
            appRenameListener = { appModel, renameLabel ->
                prefs.setAppRenameLabel(appModel.appPackage, renameLabel)
                viewModel.getAppList()
            }
        )

        linearLayoutManager = object : LinearLayoutManager(requireContext()) {
            override fun scrollVerticallyBy(
                dx: Int,
                recycler: Recycler,
                state: RecyclerView.State,
            ): Int {
                val scrollRange = super.scrollVerticallyBy(dx, recycler, state)
                val overScroll = dx - scrollRange
                if (overScroll < -10 && binding.recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING)
                    checkMessageAndExit()
                return scrollRange
            }
        }

        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(getRecyclerViewOnScrollListener())
        binding.recyclerView.itemAnimator = null
        if (requireContext().isEinkDisplay().not())
            binding.recyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)

        // Build initial letter positions once list is ready
        rebuildLetterPositions()
    }

    private fun initLetterIndex() {
        // Populate A–Z index vertically
        binding.letterIndex.removeAllViews()
        letters.forEach { ch ->
            val tv = layoutInflater.inflate(R.layout.item_letter_index, binding.letterIndex, false) as TextView
            tv.text = ch.toString()
            binding.letterIndex.addView(tv)
        }

        // Touch handler maps Y to letter, scrolls and shows overlay
        binding.letterIndex.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val letter = letterForY(v.height, event.y)
                    if (letter != null) {
                        showLetterOverlay(letter)
                        scrollToLetter(letter)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    overlayHideHandler.removeCallbacks(overlayHideRunnable)
                    overlayHideHandler.postDelayed(overlayHideRunnable, 600)
                    true
                }
                else -> false
            }
        }
    }

    private fun letterForY(viewHeight: Int, y: Float): Char? {
        if (viewHeight <= 0) return null
        val slot = (y / viewHeight * letters.size).toInt().coerceIn(0, letters.size - 1)
        return letters[slot]
    }

    private fun showLetterOverlay(letter: Char) {
        binding.letterOverlay.text = letter.toString()
        binding.letterOverlay.visibility = View.VISIBLE
        overlayHideHandler.removeCallbacks(overlayHideRunnable)
    }

    private fun rebuildLetterPositions() {
        letterPositions.clear()
        val list = adapter.appFilteredList
        for (i in list.indices) {
            val ch = normalizedFirstChar(list[i].appLabel)
            if (ch != null && ch in 'A'..'Z' && ch !in letterPositions) {
                letterPositions[ch] = i
            }
        }
    }

    private fun normalizedFirstChar(label: String): Char? {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return null
        val base = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .trim()
        return base.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()
    }

    private fun scrollToLetter(letter: Char) {
        letterPositions[letter]?.let { pos ->
            linearLayoutManager.scrollToPositionWithOffset(pos, 0)
        }
    }

    private fun showDelayStepper(packageName: String) {
        try {
            var value = prefs.getPerAppDelaySeconds(packageName) ?: (prefs.mindfulDelayMs / 1000)
            val dlg = AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.mindful_delay))
                .setMessage(packageName)
                .setPositiveButton(R.string.okay) { _, _ ->
                    prefs.setPerAppDelaySeconds(packageName, value)
                }
                .setNegativeButton(R.string.close, null)
                .setView(LayoutInflater.from(requireContext()).inflate(R.layout.view_delay_stepper, null))
                .create()
            dlg.show()
            val tvVal = dlg.findViewById<TextView>(R.id.tvValue)
            val minus = dlg.findViewById<View>(R.id.btnMinus)
            val plus = dlg.findViewById<View>(R.id.btnPlus)
            tvVal?.text = value.toString()
            minus?.setOnClickListener { value = (value - 1).coerceIn(0, 10); tvVal?.text = value.toString() }
            plus?.setOnClickListener { value = (value + 1).coerceIn(0, 10); tvVal?.text = value.toString() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initObservers() {
        viewModel.firstOpen.observe(viewLifecycleOwner) {
            if (it && flag == Constants.FLAG_LAUNCH_APP) {
                binding.appDrawerTip.visibility = View.VISIBLE
                binding.appDrawerTip.isSelected = true
            }
        }
        if (flag == Constants.FLAG_HIDDEN_APPS) {
            viewModel.hiddenApps.observe(viewLifecycleOwner) {
                it?.let {
                    adapter.setAppList(it.toMutableList())
                }
            }
        } else {
            viewModel.appList.observe(viewLifecycleOwner) {
                it?.let { appModels ->
                    adapter.setAppList(appModels.toMutableList())
                    adapter.filter.filter(binding.search.query)
                    binding.recyclerView.post { rebuildLetterPositions() }
                }
            }
        }
    }

    private fun initClickListeners() {
        binding.appDrawerTip.setOnClickListener {
            binding.appDrawerTip.isSelected = false
            binding.appDrawerTip.isSelected = true
        }
        binding.appRename.setOnClickListener {
            val name = binding.search.query.toString().trim()
            if (name.isEmpty()) {
                requireContext().showToast(getString(R.string.type_a_new_app_name_first))
                binding.search.showKeyboard()
                return@setOnClickListener
            }

            when (flag) {
                Constants.FLAG_SET_HOME_APP_1 -> prefs.appName1 = name
                Constants.FLAG_SET_HOME_APP_2 -> prefs.appName2 = name
                Constants.FLAG_SET_HOME_APP_3 -> prefs.appName3 = name
                Constants.FLAG_SET_HOME_APP_4 -> prefs.appName4 = name
                Constants.FLAG_SET_HOME_APP_5 -> prefs.appName5 = name
                Constants.FLAG_SET_HOME_APP_6 -> prefs.appName6 = name
                Constants.FLAG_SET_HOME_APP_7 -> prefs.appName7 = name
                Constants.FLAG_SET_HOME_APP_8 -> prefs.appName8 = name
            }
            findNavController().popBackStack()
        }
    }

    private fun getRecyclerViewOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {

            var onTop = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {

                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop)
                            binding.search.hideKeyboard()
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1))
                            binding.search.hideKeyboard()
                        else if (!recyclerView.canScrollVertically(-1))
                            if (!onTop && isRemoving.not())
                                binding.search.showKeyboard(prefs.autoShowKeyboard)
                    }
                }
            }
        }
    }

    private fun checkMessageAndExit() {
        findNavController().popBackStack()
        if (flag == Constants.FLAG_LAUNCH_APP)
            viewModel.checkForMessages.call()
    }

    override fun onStart() {
        super.onStart()
        binding.search.showKeyboard(prefs.autoShowKeyboard)
    }

    override fun onStop() {
        binding.search.hideKeyboard()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
