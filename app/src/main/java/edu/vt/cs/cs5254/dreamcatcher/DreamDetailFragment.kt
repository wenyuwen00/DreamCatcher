package edu.vt.cs.cs5254.dreamcatcher

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.CheckBox
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs.cs5254.dreamcatcher.databinding.FragmentDreamDetailBinding
import edu.vt.cs.cs5254.dreamcatcher.databinding.ListItemDreamEntryBinding
import edu.vt.cs.cs5254.dreamcatcher.util.CameraUtil
import java.io.File
import java.text.DateFormat
import java.util.*


private const val TAG = "DreamDetailFragment"
private const val ARG_DREAM_ID = "dream_id"
private const val DIALOG_ADD_REFLECTION = "DialogAddReflection"
private const val REQUEST_ADD_REFLECTION = 0

class DreamDetailFragment : Fragment(), AddReflectionDialog.Callbacks {
    private lateinit var dreamWithEntries: DreamWithEntries
    private var photoFile: File = File("")
    private lateinit var photoUri: Uri


    private var _binding: FragmentDreamDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DreamDetailViewModel by lazy {
        ViewModelProvider(this).get(DreamDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dreamWithEntries = DreamWithEntries(Dream(), emptyList())

        val dreamId: UUID = arguments?.getSerializable(ARG_DREAM_ID) as UUID
        viewModel.loadDream(dreamId)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_dream_detail, menu)
        val cameraAvailable = CameraUtil.isCameraAvailable(requireActivity())
        val menuItem = menu.findItem(R.id.take_dream_photo)
        menuItem.apply {
            Log.d(TAG, "Camera available: $cameraAvailable")
            isEnabled = cameraAvailable
            isVisible = cameraAvailable
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.take_dream_photo -> {
                val captureImageIntent =
                    CameraUtil.createCaptureImageIntent(requireActivity(), photoUri)
                startActivity(captureImageIntent)
                true
            }
            R.id.share_dream -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getDreamReport())
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.dream_report_subject)
                    )
                }.also { intent ->
                    val chooserIntent =
                        Intent.createChooser(intent, getString(R.string.send_report))
                    startActivity(chooserIntent)
                }
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun getDreamReport(): String {
        // create string parts
        val newline = System.getProperty("line.separator")
        val df = android.text.format.DateFormat.getMediumDateFormat(activity)
        val fulfilledString = if (dreamWithEntries.dream.isFulfilled) {
            getString(R.string.dream_report_fulfilled)
        } else if (dreamWithEntries.dream.isDeferred) {
            getString(R.string.dream_report_deferred)
        } else {
            "Dream is on Progress"
        }

        val dateString = df.format(dreamWithEntries.dream.date)
        val dateMessage = getString(R.string.dream_report_date, dateString)
        var reflectionString = getString(R.string.dream_report_ref)
        dreamWithEntries.dreamEntries.forEach {
            if (it.kind == DreamEntryKind.REFLECTION) {
                reflectionString += " $newline - ${it.text}"
            }
        }
        // create and return complete string
        val sb = StringBuilder()
        sb.append(">> ${dreamWithEntries.dream.title} << $newline")
        sb.append("$dateMessage $newline")
        sb.append("$reflectionString $newline")
        sb.append("$fulfilledString $newline")
        return sb.toString()
    }

    private var adapter: DreamEntryAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDreamDetailBinding.inflate(inflater, container, false)
        val view = binding.root
        refreshView(dreamWithEntries.dreamEntries)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var itemTouchHelper = ItemTouchHelper(DreamEntrySwipeToDeleteCallback(adapter!!))
        itemTouchHelper.attachToRecyclerView(binding.dreamEntryRecyclerView)

        viewModel.dreamLiveData.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { dream ->
                dream?.let {
                    this.dreamWithEntries = dream
                    photoFile = viewModel.getPhotoFile(dream)
                    photoUri = FileProvider.getUriForFile(
                        requireActivity(),
                        "com.bignerdranch.android.criminalintent.fileprovider",
                        photoFile
                    )
                    binding.dreamEntryRecyclerView.layoutManager = LinearLayoutManager(context)
                    binding.dreamEntryRecyclerView.adapter = adapter
                    var conceivedEntry = DreamEntry(
                        UUID.randomUUID(),
                        Date(),
                        "Conceived",
                        DreamEntryKind.CONCEIVED,
                        dream.dream.id
                    )
                    if(dream.dreamEntries.size<1){
                        dream.dreamEntries =
                            dream.dreamEntries.plus(conceivedEntry)

                    }

                    refreshView(dream.dreamEntries)
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?, start: Int, count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int, before: Int, count: Int
            ) {
                dreamWithEntries.dream.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {}
        }
        binding.dreamTitleText.addTextChangedListener(titleWatcher)
        binding.dreamFulfilledCheckbox.setOnClickListener { v ->
            val isCheck = (v as CheckBox).isChecked
            dreamWithEntries.dream.isFulfilled = isCheck
            binding.dreamDeferredCheckbox.isEnabled = !isCheck
            when (isCheck) {
                true
                -> {
                    var fulfilledEntry = DreamEntry(
                        UUID.randomUUID(),
                        Date(),
                        "",
                        DreamEntryKind.FULFILLED,
                        dreamWithEntries.dream.id
                    )
                    dreamWithEntries.dreamEntries =
                        dreamWithEntries.dreamEntries.plus(fulfilledEntry)
                    binding.addReflectionButton.isEnabled = false
                }
                else
                -> {
                    dreamWithEntries.dreamEntries =
                        dreamWithEntries.dreamEntries.minus(
                            dreamWithEntries.dreamEntries.filter { it.kind == DreamEntryKind.FULFILLED }
                        )
                    binding.addReflectionButton.isEnabled = true
                }
            }
            refreshView(dreamWithEntries.dreamEntries)
        }

        binding.dreamDeferredCheckbox.setOnClickListener { v ->
            val isCheck = (v as CheckBox).isChecked
            dreamWithEntries.dream.isDeferred = isCheck
            binding.dreamFulfilledCheckbox.isEnabled = !isCheck
            when (isCheck) {
                true
                -> {
                    var deferredEntry = DreamEntry(
                        UUID.randomUUID(),
                        Date(),
                        "",
                        DreamEntryKind.DEFERRED,
                        dreamWithEntries.dream.id
                    )
                    dreamWithEntries.dreamEntries =
                        dreamWithEntries.dreamEntries.plus(deferredEntry)
                }
                else
                ->
                    dreamWithEntries.dreamEntries =
                        dreamWithEntries.dreamEntries.minus(
                            dreamWithEntries.dreamEntries.filter { it.kind == DreamEntryKind.DEFERRED }
                        )
            }
            refreshView(dreamWithEntries.dreamEntries)

        }
        binding.addReflectionButton.setOnClickListener {
            AddReflectionDialog().apply {
                setTargetFragment(this@DreamDetailFragment, REQUEST_ADD_REFLECTION)
                show(this@DreamDetailFragment.parentFragmentManager, DIALOG_ADD_REFLECTION)
            }
        }
    }

    override fun onReflectionProvided(text: String) {

        var reflectionEntry = DreamEntry(
            UUID.randomUUID(),
            Date(),
            text,
            DreamEntryKind.REFLECTION,
            dreamWithEntries.dream.id
        )
        dreamWithEntries.dreamEntries =
            dreamWithEntries.dreamEntries.plus(reflectionEntry)

        refreshView(dreamWithEntries.dreamEntries)
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveDream(dreamWithEntries)
    }


    private fun refreshView(dreamEntries: List<DreamEntry>) {
        // adapter
        adapter = DreamEntryAdapter(dreamEntries)
        binding.dreamEntryRecyclerView.adapter = adapter

        binding.dreamTitleText.setText(dreamWithEntries.dream.title)

        when (dreamWithEntries.dream.isFulfilled) {
            true -> {
                binding.dreamFulfilledCheckbox.isChecked =
                    dreamWithEntries.dream.isFulfilled
                binding.dreamFulfilledCheckbox.jumpDrawablesToCurrentState()
                binding.dreamDeferredCheckbox.isEnabled = false
                binding.addReflectionButton.isEnabled = false
            }
        }
        when (dreamWithEntries.dream.isDeferred) {
            true -> {
                binding.dreamDeferredCheckbox.isChecked =
                    dreamWithEntries.dream.isDeferred
                binding.dreamDeferredCheckbox.jumpDrawablesToCurrentState()
                binding.dreamFulfilledCheckbox.isEnabled = false
            }
        }
        updatePhotoView()
    }

    inner class DreamEntryHolder(val itemBinding: ListItemDreamEntryBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        private lateinit var dreamEntry: DreamEntry

        init {
        }

        fun bind(dreamEntry: DreamEntry) {
            this.dreamEntry = dreamEntry
            updateEntryButton(itemBinding.dreamEntryButton, dreamEntry)
        }

    }

    private inner class DreamEntryAdapter(var entries: List<DreamEntry>) :
        RecyclerView.Adapter<DreamEntryHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DreamEntryHolder {
            val itemBinding = ListItemDreamEntryBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return DreamEntryHolder(itemBinding)
        }

        override fun getItemCount() = entries.size
        override fun onBindViewHolder(holder: DreamEntryHolder, position: Int) {
            val dreamEntry = entries[position]
            holder.bind(dreamEntry)
        }

        fun deleteItem(pos: Int) {
            var entry = dreamWithEntries.dreamEntries.get(pos)
            if (entry.kind == DreamEntryKind.REFLECTION){
                dreamWithEntries.dreamEntries =
                    dreamWithEntries.dreamEntries.minus(entry)

            }
            refreshView(dreamWithEntries.dreamEntries)

        }
    }

    private inner class DreamEntrySwipeToDeleteCallback(var adapter: DreamEntryAdapter) :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            TODO("Not yet implemented")
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            var pos = viewHolder.adapterPosition
            adapter.deleteItem(pos)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateEntryButton(button: Button, entry: DreamEntry) {
        when (entry.kind) {
            DreamEntryKind.REFLECTION
            -> button.text =
                DateFormat.getDateInstance(DateFormat.MEDIUM)
                    .format(entry.date) + ": " + entry.text
            else -> button.text = entry.kind.toString()
        }
        when (entry.kind) {
            DreamEntryKind.CONCEIVED
            -> button.visibility = View.VISIBLE
            DreamEntryKind.REFLECTION
            -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.design_default_color_secondary_variant,
                        null
                    )
                )
                button.visibility = View.VISIBLE
            }
            DreamEntryKind.FULFILLED
            -> {
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.design_default_color_primary_variant,
                        null
                    )
                )
                button.visibility = View.VISIBLE
            }
            DreamEntryKind.DEFERRED
            -> {
                button.setBackgroundColor(Color.RED)
                button.visibility = View.VISIBLE
            }
        }
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = CameraUtil.getScaledBitmap(photoFile.path, requireActivity())
            binding.dreamPhoto.setImageBitmap(bitmap)
        } else {
            binding.dreamPhoto.setImageDrawable(null)
        }
    }

    companion object {
        fun newInstance(crimeId: UUID): DreamDetailFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DREAM_ID, crimeId)
            }
            return DreamDetailFragment().apply {
                arguments = args
            }
        }
    }
}