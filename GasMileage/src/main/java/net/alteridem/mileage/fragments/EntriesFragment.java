package net.alteridem.mileage.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import net.alteridem.mileage.R;
import net.alteridem.mileage.VehicleActivity;
import net.alteridem.mileage.adapters.EntriesAdapter;
import net.alteridem.mileage.data.Entry;
import net.alteridem.mileage.data.Vehicle;

import java.util.List;

/**
 * Created by Robert Prouse on 13/06/13.
 */
public class EntriesFragment extends Fragment {
    ListView _vehicleEntries;
    EntriesAdapter _adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entries, container, false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.entries_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if ( info != null ) {
            long entry_id = _adapter.getItemId(info.position);
            if ( entry_id >= 0 ) {
                switch (item.getItemId()) {
                    case R.id.entry_menu_edit:
                        editEntry(entry_id);
                        return true;
                    case R.id.entry_menu_delete:
                        deleteEntry(entry_id);
                        return true;
                }
            }
        }
        return super.onContextItemSelected(item);
    }

    private void editEntry(long id) {
        VehicleActivity activity = (VehicleActivity) getActivity();
        if (activity != null) {
            activity.editFillUp(id);
        }
    }

    private void deleteEntry(long id) {
        Entry.delete(id);
        VehicleActivity activity = (VehicleActivity) getActivity();
        if (activity != null) {
            Vehicle vehicle = activity.getCurrentVehicle();
            if ( vehicle != null ) {
                vehicle.updateLastMileage();
            }
            fillEntries(activity.getEntries());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        _vehicleEntries = (ListView) getActivity().findViewById(R.id.vehicle_entries);
        registerForContextMenu(_vehicleEntries);
        fillEntries();
    }

    private void fillEntries() {
        VehicleActivity activity = (VehicleActivity) getActivity();
        if (activity != null) {
            fillEntries(activity.getEntries());
        }
    }

    public void fillEntries(List<Entry> entries) {
        if (entries == null || getActivity() == null )
            return;

        // fill in the grid_item layout
        _adapter = new EntriesAdapter(getActivity(), entries);
        _vehicleEntries.setAdapter(_adapter);
    }
}
