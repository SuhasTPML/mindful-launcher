package app.olauncher.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.olauncher.R
import app.olauncher.data.Prefs
import android.widget.TextView
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import app.olauncher.data.Constants

class ManageDelayedAppsFragment : Fragment() {
    private lateinit var prefs: Prefs

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manage_delayed_apps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())

        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = Adapter()

        view.findViewById<View>(R.id.btnAdd)?.setOnClickListener {
            findNavController().navigate(
                R.id.appListFragment,
                bundleOf(Constants.Key.FLAG to Constants.FLAG_SELECT_DELAY_APP)
            )
        }
    }

    inner class Adapter : RecyclerView.Adapter<VH>() {
        private val pm: PackageManager = requireContext().packageManager
        private fun labelOf(pkg: String): String = try {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) { pkg }

        private fun items(): List<String> = prefs.mindfulDelayedApps.toList().sortedBy { labelOf(it).lowercase() }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_delayed_app, parent, false)
            return VH(v)
        }
        override fun getItemCount(): Int = items().size
        override fun onBindViewHolder(holder: VH, position: Int) {
            val pkg = items()[position]
            holder.bind(pkg, labelOf(pkg))
        }
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvName: TextView = v.findViewById(R.id.tvName)
        private val tvDelay: TextView = v.findViewById(R.id.tvDelay)
        private val btnEdit: Button = v.findViewById(R.id.btnEdit)
        private val btnDisable: Button = v.findViewById(R.id.btnDisable)

        fun bind(pkg: String, label: String) {
            tvName.text = label
            val per = prefs.getPerAppDelaySeconds(pkg)
            val sec = per ?: (prefs.mindfulDelayMs / 1000)
            tvDelay.text = itemView.context.getString(R.string.starting_in_seconds, sec)
            btnEdit.setOnClickListener { showEditDialog(pkg, label, per ?: sec) }
            btnDisable.setOnClickListener {
                val set = prefs.mindfulDelayedApps
                set.remove(pkg)
                prefs.mindfulDelayedApps = set
                prefs.removePerAppDelay(pkg)
                (itemView.parent as? RecyclerView)?.adapter?.notifyDataSetChanged()
            }
        }

        private fun showEditDialog(pkg: String, label: String, currentSec: Int) {
            var value = currentSec
            val dlg = AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.mindful_delay))
                .setMessage(label)
                .setPositiveButton(R.string.okay) { _, _ ->
                    prefs.setPerAppDelaySeconds(pkg, value)
                    (itemView.parent as? RecyclerView)?.adapter?.notifyDataSetChanged()
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
        }
    }
}
