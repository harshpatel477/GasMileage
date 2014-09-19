package net.alteridem.mileage;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import net.alteridem.mileage.adapters.VehicleSpinnerAdapter;
import net.alteridem.mileage.data.Entry;
import net.alteridem.mileage.data.Vehicle;
import net.alteridem.mileage.fragments.EntriesFragment;
import net.alteridem.mileage.fragments.StatisticsFragment;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ItemSelect;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;

@EActivity(R.layout.activity_vehicle)
@OptionsMenu(R.menu.vehicle)
public class VehicleActivity extends Activity implements VehicleDialog.IVehicleDialogListener, EntryDialog.IEntryDialogListener {

    private static final String TAG = VehicleActivity.class.getSimpleName();

    @App
    MileageApplication _application;

    @ViewById(R.id.vehicle_name) Spinner _spinner;

    List<Vehicle> _vehicleList;
    Vehicle _currentVehicle;
    Boolean _landscape;

    @AfterViews
    void init() {
        // Subscribe to the preferences changing
        MileageApplication.getSharedPreferences().registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                loadData();
            }
        });

        _landscape = findViewById(R.id.vehicle_fragment) == null;

        if ( !_landscape ) {
            // Set up the action bar to show a dropdown list.
            final ActionBar actionBar = getActionBar();
            if ( actionBar == null ) return;

            //actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            ActionBar.Tab tab = actionBar.newTab()
                    .setText( R.string.vehicle_statistics )
                    .setTabListener(new TabListener<StatisticsFragment>(this, "statistics", StatisticsFragment.class));
            actionBar.addTab( tab );

            tab = actionBar.newTab()
                    .setText( R.string.vehicle_entries )
                    .setTabListener(new TabListener<EntriesFragment>(this, "entries", EntriesFragment.class));
            actionBar.addTab( tab );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    @ItemSelect(R.id.vehicle_name)
    void onVehicleSelecte(boolean selected, int position) {
        _currentVehicle = (Vehicle) _spinner.getSelectedItem();
        saveLastVehicle();
        loadVehicle();
    }

    @OptionsItem(R.id.menu_help)
    void showHelp() {
        startActivity( new Intent( this, HelpActivity_.class ) );
    }

    @OptionsItem(R.id.menu_settings)
    void showSettings() {
        startActivity( new Intent( this, MileagePreferencesActivity.class ) );
    }

    @OptionsItem(R.id.menu_fill_up)
    void enterFillUp() {
        Log.d(TAG, "enterFillUp");
        FragmentManager fm = getFragmentManager();
        EntryDialog dlg = new EntryDialog( _currentVehicle );
        dlg.show(fm, "entry_dialog");
    }

    @OptionsItem(R.id.menu_add_vehicle)
    void addVehicle() {
        Log.d( TAG, "addVehicle" );
        FragmentManager fm = getFragmentManager();
        VehicleDialog dlg = new VehicleDialog();
        dlg.show(fm, "add_vehicle_dialog");
    }

    @OptionsItem(R.id.menu_edit_vehicle)
    void editVehicle() {
        Log.d( TAG, "editVehicle" );
        FragmentManager fm = getFragmentManager();
        VehicleDialog dlg = new VehicleDialog( _currentVehicle );
        dlg.show( fm, "edit_vehicle_dialog" );
    }

    @OptionsItem(R.id.menu_delete_vehicle)
    void deleteVehicle() {
        // Can't delete the last vehicle
        if( _vehicleList.size() <= 1 ) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete_vehicle_dialog_title)
                    .setMessage(R.string.delete_vehicle_dialog_cant_delete)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        } else {
            // Are you sure?
            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete_vehicle_dialog_title)
                    .setMessage(R.string.delete_vehicle_dialog_text)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            _currentVehicle.delete();
                            loadData();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
        Log.d(TAG, "deleteVehicle");
    }

    public Vehicle getCurrentVehicle()
    {
        return _currentVehicle;
    }

    public List<Entry> getEntries() {
        List<Entry> entries;
        if ( _currentVehicle != null ) {
            entries = _currentVehicle.getEntries();
        } else {
            entries = new ArrayList<Entry>();
        }
        return entries;
    }

    private void loadData() {
        loadVehicles();
        loadVehicle();
    }

    private void loadVehicles() {
        _vehicleList = Vehicle.fetchAll();
        ArrayAdapter adapter_veh = new VehicleSpinnerAdapter( this, _vehicleList );
        _spinner.setAdapter( adapter_veh );

        loadLastVehicle();
    }

    private void loadLastVehicle() {
        long vehId = MileageApplication.getSharedPreferences().getLong( "last_vehicle", -1 );
        if ( vehId == -1 )
            _spinner.setSelection( 0 );
        else
            switchToVehicle(vehId);
    }

    private void saveLastVehicle() {
        long vehId = -1;
        if ( _currentVehicle != null ) {
            vehId = _currentVehicle.getId();
        }
        SharedPreferences.Editor edit = MileageApplication.getSharedPreferences().edit();
        edit.putLong( "last_vehicle", vehId );
        edit.commit();
    }

    public void editFillUp(long entry_id) {
        Log.d(TAG, "editFillUp");
        Entry entry = Entry.fetch(entry_id);
        if ( entry != null ) {
            FragmentManager fm = getFragmentManager();
            EntryDialog dlg = new EntryDialog( _currentVehicle, entry );
            dlg.show(fm, "entry_edit_dialog");
        }
    }

    public void deleteFillUp(long entry_id){
        Entry.delete(entry_id);
        Vehicle vehicle = getCurrentVehicle();
        if ( vehicle != null ) {
            vehicle.updateLastMileage();
            switchToVehicle( vehicle.getId() );
        }
    }

    public void switchToVehicle(long vehicle_id) {
        Vehicle v = null;
        for( Vehicle veh : _vehicleList ) {
            if ( veh.getId() == vehicle_id ) {
                veh.reload();
                v = veh;
                break;
            }
        }
        int pos = _vehicleList.indexOf( v );
        if ( pos >= 0 ) {
            _spinner.setSelection( pos );
        }
        loadVehicle();
    }

    private void loadVehicle() {
        _currentVehicle = (Vehicle) _spinner.getSelectedItem();

        if ( !_landscape ) {
            ActionBar actionBar = getActionBar();
            if ( actionBar == null ) return;

            ActionBar.Tab tab = actionBar.getSelectedTab();
            if ( tab == null ) return;

            // This will load the data into the current fragment
            setDataToFragment((Fragment) tab.getTag());
        } else {
            StatisticsFragment stats = (StatisticsFragment)getFragmentManager().findFragmentById(R.id.fragment_statistics);
            setDataToFragment(stats);
            EntriesFragment entries = (EntriesFragment)getFragmentManager().findFragmentById(R.id.fragment_entries);
            setDataToFragment(entries);
        }
    }

    public void setDataToFragment( Fragment fragment ) {
        if ( fragment == null )
            return;

        if ( fragment.getClass() == StatisticsFragment.class ) {
            StatisticsFragment sf = (StatisticsFragment)fragment;
            sf.fillStatistics( _currentVehicle );
        } else if ( fragment.getClass() == EntriesFragment.class ) {
            EntriesFragment ef = (EntriesFragment)fragment;
            ef.fillEntries( getEntries() );
        }
    }

    @Override
    public void onFinishVehicleDialog( Vehicle vehicle ) {
        // Save the vehicle to the last vehicle used so we can set the
        // spinner to it
        _currentVehicle = vehicle;
        saveLastVehicle();
        loadVehicles();
    }

    @Override
    public void onFinishEntryDialog( Vehicle vehicle ) {
        switchToVehicle( vehicle.getId() );

        // Determine if we should show this dialog
        if ( MileageApplication.getSharedPreferences().getBoolean( "show_reset_odometer", true ) ) {
            FragmentManager fm = getFragmentManager();
            ResetOdometerDialog dlg = new ResetOdometerDialog();
            dlg.show( fm, "reset_odometer_dialog" );
        }
    }
}
