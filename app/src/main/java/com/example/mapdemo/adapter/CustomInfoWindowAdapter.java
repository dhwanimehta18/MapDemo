package com.example.mapdemo.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;

import com.example.mapdemo.R;
import com.example.mapdemo.model.InfoWindowData;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;


public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private Context context;

    public CustomInfoWindowAdapter(Context ctx) {
        context = ctx;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        View view = ((Activity) context).getLayoutInflater()
                .inflate(R.layout.custom_info_view, null);

        InfoWindowData infoWindowData = (InfoWindowData) marker.getTag();
        AppCompatTextView tvStreetName, tvCity, tvState, tvCountry, tvPostalCode;
        tvCity = view.findViewById(R.id.tv_city);
        tvCountry = view.findViewById(R.id.tv_country);
        tvPostalCode = view.findViewById(R.id.tv_postal_code);
        tvState = view.findViewById(R.id.tv_state);
        tvStreetName = view.findViewById(R.id.tv_street_name);

        if (infoWindowData != null) {
            tvStreetName.setText(infoWindowData.getStreet());
            tvCity.setText(infoWindowData.getCity());
            tvState.setText(infoWindowData.getState());
            tvCountry.setText(infoWindowData.getCountry());
            tvPostalCode.setText(infoWindowData.getPostal_code());
        }
        return view;
    }
}
