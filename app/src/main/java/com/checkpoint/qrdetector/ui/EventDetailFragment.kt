package com.checkpoint.qrdetector.ui

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.checkpoint.qrdetector.databinding.FragmentEventDetailBinding
import com.checkpoint.qrdetector.utils.CacheFile

private const val DATE = "date"
private const val TRANSLATION = "translation"
private const val DIRECTION = "direction"
private const val ID_IMAGE = "idImage"

/**
 * A simple [Fragment] subclass.
 * Use the [EventDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EventDetailFragment : Fragment() {

    private var translation: String? = null
    private var direction: String? = null
    private var idImage: String? = null
    private var date: String? = null
    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!
    private var cacheFile:   CacheFile? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            date = it.getString(DATE)
            translation = it.getString(TRANSLATION)
            direction = it.getString(DIRECTION)
            idImage = it.getString(ID_IMAGE)
        }
        cacheFile = CacheFile(requireContext())
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        binding.textDate.text= date
        binding.textTranslationCode.text = translation
        binding.textDirectionDetected.text = direction
        Log.e("translation",translation!!)
        Log.e("direction",direction!!)

        var bitmap = cacheFile!!.getImageFromCache("$idImage")
        binding.imgDetection.setImageBitmap(bitmap)
        binding.textDate.setOnClickListener {
         }
        return binding.root

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param date Date of event occurs
         * @param direction Direction estimation from image analysis
         * @param translation QR code content on human readable format
         * @param idImage  Name of cached image of detection event
         * @return A new instance of fragment EventDetailFragment.
         *
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(date: String, direction: String,translation: String,idImage: String) =
            EventDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(DATE, date)
                    putString(DIRECTION, direction)
                    putString(TRANSLATION, translation)
                    putString(ID_IMAGE, idImage)
                }
            }
    }
}