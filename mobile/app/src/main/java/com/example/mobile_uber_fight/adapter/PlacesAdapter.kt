package com.example.mobile_uber_fight.adapter

import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.concurrent.TimeUnit

class PlacesAdapter(context: Context, private val placesClient: PlacesClient) :
    ArrayAdapter<String>(context, android.R.layout.simple_expandable_list_item_1), Filterable {

    private var resultList: List<PlaceAutocomplete> = ArrayList()

    override fun getCount(): Int = resultList.size

    override fun getItem(index: Int): String = resultList[index].address

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                if (constraint != null) {
                    resultList = getAutocomplete(constraint)
                    filterResults.values = resultList
                    filterResults.count = resultList.size
                }
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    private fun getAutocomplete(constraint: CharSequence): List<PlaceAutocomplete> {
        val list = mutableListOf<PlaceAutocomplete>()
        val token = AutocompleteSessionToken.newInstance()

        val request = FindAutocompletePredictionsRequest.builder()
            .setCountries("FR")
            .setTypesFilter(listOf(PlaceTypes.ADDRESS))
            .setSessionToken(token)
            .setQuery(constraint.toString())
            .build()

        try {
            val task = placesClient.findAutocompletePredictions(request)
            val response = Tasks.await(task, 60, TimeUnit.SECONDS)
            
            for (prediction in response.autocompletePredictions) {
                list.add(
                    PlaceAutocomplete(
                        prediction.placeId,
                        prediction.getFullText(null).toString()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("PlacesAdapter", "Error getting autocomplete prediction", e)
        }

        return list
    }

    fun getPlaceId(position: Int): String {
        return resultList[position].placeId
    }

    data class PlaceAutocomplete(val placeId: String, val address: String)
}