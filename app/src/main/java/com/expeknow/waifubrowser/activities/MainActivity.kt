package com.expeknow.waifubrowser.activities


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.expeknow.waifubrowser.utils.Constants
import com.expeknow.waifubrowser.R
import com.expeknow.waifubrowser.databinding.ActivityMainBinding
import com.expeknow.waifubrowser.databinding.ActivityOptionsBinding
import com.squareup.picasso.Picasso

import org.json.JSONArray
import kotlin.math.abs


class MainActivity : AppCompatActivity(),
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener, AdapterView.OnItemSelectedListener {

    private var binding : ActivityMainBinding? = null
    private lateinit var bindingDialog : ActivityOptionsBinding

    /**
     * Used to pass the current image link in Piccasso to show image
     */
    private var currImageUrl : String? = null


    private var sp : SharedPreferences? = null
    private var editor : SharedPreferences.Editor? = null

    private var isUserInteracting: Boolean = false
    private var isCategoryChanged: Boolean = false
    var swipeUp = true
    var swipeDown = false

    private lateinit var gestureDetector: GestureDetectorCompat
    private var x1 : Float = 0.0f
    private var x2 : Float = 0.0f
    private var y1 : Float = 0.0f
    private var y2 : Float = 0.0f

    private var indexForWaifuIm : Int = -1
    private var waifuImObjects = JSONArray()
    private var indexForWaifuPics : Int = -1
    private var waifuPicsImagesLink = ArrayList<String>()


    private companion object{
        const val WAIFU_IM = true       //link 2
        const val WAIFU_PICS = false    //link 1
    }

    /**
     * Current URL to make API call to
     */
    private var url : String = Constants.sfwLink1
    private var selectedCategory : String = "waifu"

    /**
     * Current category list to show in Options Dialog
     */
    var category : Array<String> = Constants.NSFW_LINK1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        gestureDetector = GestureDetectorCompat(this,this)
        gestureDetector.setOnDoubleTapListener(this)


        sp = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val switch1State = sp?.getBoolean("switch1", false)
        val switch2State = sp?.getBoolean("switch2", false)

        setLinkForSettings(switch2State!!, switch1State!!)
        serverStateChanger(switch1State)
        nsfwStateChanger(switch2State)


        binding?.optionsButton?.setOnClickListener {
            showOptionsDialog()
        }

        binding?.refreshButton?.setOnClickListener {
            onRefreshButtonPress()
        }

        binding?.downloadButton?.setOnClickListener {
            downloadImage()
        }

        refreshWaifu(swipeUp)

    }

    /**
     * Shows the server and NSFW change switches and also saves the changes in those switches
     * in PreferenceFile so they can be retrieved later.
     */
    private fun showOptionsDialog(){
        val optionsDialog = Dialog(this)
        bindingDialog = ActivityOptionsBinding.inflate(layoutInflater)
        optionsDialog.setContentView(bindingDialog.root)
        optionsDialog.setTitle("Options")

        sp = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        editor = sp?.edit()

        bindingDialog.categorySelector.onItemSelectedListener = this

        categoryStateChanger(category)

        val sharedPreferences = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val switch1State = sharedPreferences.getBoolean("switch1", false)
        val switch2State = sharedPreferences.getBoolean("switch2", false)

        bindingDialog.serverSwitch.isChecked = switch1State
        bindingDialog.nsfwSwitch.isChecked = switch2State

        bindingDialog.serverSwitch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            editor?.putBoolean("switch1", isChecked)
            editor?.apply()
            if(!isChecked){
                when(url){
                    Constants.sfwLink2 -> {
                        url = Constants.sfwLink1
                        category = Constants.SFW_LINK1
                    }
                    Constants.nsfwLink2 -> {
                        url = Constants.nsfwLink1
                        category = Constants.NSFW_LINK1
                    }
                }
            }
            else{
                when(url){
                    Constants.sfwLink1 -> {
                        url = Constants.sfwLink2
                        category = Constants.SFW_LINK2
                    }
                    Constants.nsfwLink1 -> {
                        url = Constants.nsfwLink2
                        category = Constants.NSFW_LINK2
                    }
                }
            }
            serverStateChanger(isChecked)
            categoryStateChanger(category)
            //waifu is a common category so when anything is changed, we switch to waifu
            selectedCategory = "waifu"
            refreshWaifu(swipeUp)
        })

        bindingDialog.nsfwSwitch.setOnCheckedChangeListener { _, isChecked ->
            editor?.putBoolean("switch2", isChecked)
            editor?.apply()
            if(!isChecked){
                if(url== Constants.nsfwLink1){
                    url = Constants.sfwLink1
                    category = Constants.SFW_LINK1
                } else {
                    url = Constants.sfwLink2
                    category = Constants.SFW_LINK2
                }
            } else{
                if(url == Constants.sfwLink1){
                    url = Constants.nsfwLink1
                    category = Constants.NSFW_LINK1
                } else {
                    url = Constants.nsfwLink2
                    category = Constants.NSFW_LINK2
                }
            }
            nsfwStateChanger(isChecked)
            categoryStateChanger(category)
            selectedCategory = "waifu"
            refreshWaifu(swipeUp)
        }
        optionsDialog.show()
        isUserInteracting = false
    }

    /**
     * Refresh waifu deletes all images that were loaded and asks for fresh batch of images
     */
    private fun onRefreshButtonPress(){
        indexForWaifuIm = -1
        waifuImObjects = JSONArray()
        indexForWaifuPics = -1
        waifuPicsImagesLink = ArrayList()
        refreshWaifu(swipeUp)

    }

    /**
     * Based on the swipe direction and current selected server, this methods gets new images
     * to show in the Imageview
     */
    @SuppressLint("SetTextI18n")
    private fun refreshWaifu(swipeDirection: Boolean)
    {
        binding?.loadingText?.text = getString(R.string.loading_waifu_text)

        if(url == Constants.sfwLink1 || url == Constants.nsfwLink1){

            if(swipeDirection == swipeUp){
                if(waifuPicsImagesLink.size == 0 || indexForWaifuPics >= waifuPicsImagesLink.size-1){
                    if(isCategoryChanged){
                        waifuPicsImagesLink.clear()
                        indexForWaifuPics = -1
                    }
                    runOnUiThread{ requestForMoreImages(WAIFU_PICS) }
                    isCategoryChanged = false
                    return
                }
                val currImageUrl = waifuPicsImagesLink.get(++indexForWaifuPics)
                setupWaifuOnView(currImageUrl)
                }
            else{
                if(indexForWaifuPics <= 0){
                    binding?.loadingText?.text = "This is the first image of this session!"
                    return
                }
                currImageUrl = waifuPicsImagesLink.get(--indexForWaifuPics)
                setupWaifuOnView(currImageUrl!!)
            }
        }else{
            if(swipeDirection == swipeUp){
                if(waifuImObjects.length() == 0 || indexForWaifuIm >= waifuImObjects.length()-1
                    || isCategoryChanged){
                    runOnUiThread{ requestForMoreImages(WAIFU_IM) }
                    isCategoryChanged = false
                    return
                }
                currImageUrl = waifuImObjects.getJSONObject(++indexForWaifuIm)?.getString("url")
                setupWaifuOnView(currImageUrl!!)
            }else{
                if(indexForWaifuIm <= 0){
                    binding?.loadingText?.text = "This is the first image of this session!"
                    return
                }
                currImageUrl = waifuImObjects.getJSONObject(--indexForWaifuIm)?.getString("url")
                setupWaifuOnView(currImageUrl!!)
            }
        }
    }

    /**
     * Based on the linkType boolean parameter, this function makes call to the API to request
     * for more waifu images
     */
    private fun requestForMoreImages(linkType: Boolean){
        val request = JsonObjectRequest(
            Request.Method.GET, url+selectedCategory, null,
            { response ->
                if(linkType == WAIFU_PICS){
                    waifuPicsImagesLink.add(response.get("url").toString())
                    currImageUrl = waifuPicsImagesLink.get(++indexForWaifuPics)
                    setupWaifuOnView(currImageUrl!!)
                    }
                else{
                    val newWaifuImObjects : JSONArray = response.get("images") as JSONArray
                    val waifuImObjectsNew = JSONArray()
                    for(i in 0 until newWaifuImObjects.length()){
                        if(newWaifuImObjects.getJSONObject(i).getLong("byte_size") < 15000000)
                            waifuImObjectsNew.put(newWaifuImObjects.getJSONObject(i))
                        }
                    waifuImObjects = waifuImObjectsNew
                    currImageUrl = waifuImObjects.getJSONObject(++indexForWaifuIm)?.getString("url")
                    setupWaifuOnView(currImageUrl!!)
                    }
                binding?.loadingText?.text = ""
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this, "No internet connection. Please check your internet" +
                        " connectivity and try again.", Toast.LENGTH_LONG).show()
                binding?.loadingText?.text = "No internet connection."
            })

        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(request)
    }

    /**
     * Download and sets the waifu in the imageview
     */
    private fun setupWaifuOnView(imageUrl: String){

        Picasso.get()
            .load(imageUrl)
            .placeholder(R.drawable.loading)
            .into(binding?.waifuView)
        binding?.loadingText?.text = ""

    }

    /**
     * Changes the text above ImageView to indicate what server is currently selected
     */
    private fun serverStateChanger(state: Boolean){
        if(state) {
            binding?.serverState?.text = "Server: Waifu.pics"
        }else{
            binding?.serverState?.text = "Server: Waifu.im"
        }
    }

    /**
     * Changes the text above ImageView to indicate what images are being loaded
     */
    private fun nsfwStateChanger(state: Boolean){
        if(state){
            binding?.nsfwState?.text = "NSFW: Enabled"
        }else{
            binding?.nsfwState?.text = "NSFW: Disabled"
        }
    }

    /**
     * In the options dialog, whenever any switch's state is changed, this function runs
     * to update the category list matching to that link
     */
    private fun categoryStateChanger(category: Array<String>){
        // Create the instance of ArrayAdapter
        // having the list of courses
        val ad: ArrayAdapter<*> = ArrayAdapter<Any?>(
            this,
            R.layout.category_spinner_item,
            category)
        // set simple layout resource file
        // for each item of spinner
        ad.setDropDownViewResource(
            R.layout.simple_spinner_dropdown_items)
        // Set the ArrayAdapter (ad) data on the
        // Spinner which binds data to spinner
        bindingDialog.categorySelector.adapter = ad
    }

    /**
     * Based on the position of server and NSFW switch, sets the url for making API calls
     */
    private fun setLinkForSettings(switch1State: Boolean, switch2State: Boolean){
        if(switch1State && switch2State){
            url = Constants.nsfwLink2
            category = Constants.NSFW_LINK2
        }
        else if(switch1State){
            url = Constants.nsfwLink1
            category = Constants.NSFW_LINK1
        }
        else if(switch2State){
            url = Constants.sfwLink2
            category = Constants.SFW_LINK2
        }
        else{
            url = Constants.sfwLink1
            category = Constants.SFW_LINK1
        }

    }

    /**
     * Downloads the image by taking the link saved in global waifu image link
     */
    @SuppressLint("SetTextI18n")
    private fun downloadImage() {
        if (currImageUrl.isNullOrBlank()){
            Toast.makeText(this, "No image present. Please check your internet to load an image.",
            Toast.LENGTH_LONG).show()
            return
        }
        binding?.loadingText?.text = "Downloading Waifu..."
        val request = DownloadManager.Request(Uri.parse(currImageUrl))
        request.setTitle("Waifu Browser") // Title for notification
        request.setDescription("Downloading Waifu...")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Waifu_image.jpg")
        val manager: DownloadManager?
        manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        binding?.loadingText?.text = ""
        Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show()
    }


    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent) {
        return
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        showOptionsDialog()
        return
    }

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        x1 = e1.x
        y1 = e1.y
        x2 = e2.x
        y2 = e2.y
        val swipeDistance : Float = y2-y1
        if(abs(swipeDistance) > Constants.MIN_DISTANCE){
            if(y2 < y1){
                refreshWaifu(swipeUp)
                return true
            }else{
                refreshWaifu(swipeDown)
                return true
            }

        }
        return false
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if(binding?.waifuView?.scaleType == ImageView.ScaleType.FIT_CENTER){
            binding?.waifuView?.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        else
            binding?.waifuView?.scaleType = ImageView.ScaleType.FIT_CENTER
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        binding?.downloadButton?.performClick()
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (gestureDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }


    /**
     * Whenever a category of image is selected, this method changes that category adapter to show
     * the selected category at the top on adapter and also saves that in Preference File
     */
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if(isUserInteracting){
            selectedCategory = category[position]
            binding?.currCategory?.text = "Category: $selectedCategory"
            editor?.putString("category", category[position])
            category[position] = category[0]
            category[0] = selectedCategory
            isCategoryChanged = true
            refreshWaifu(swipeUp)
        }
        isUserInteracting = true
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }


}

