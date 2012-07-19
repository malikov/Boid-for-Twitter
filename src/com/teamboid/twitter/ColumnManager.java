package com.teamboid.twitter;

import java.util.ArrayList;

import twitter4j.ResponseList;
import twitter4j.SavedSearch;
import twitter4j.TwitterException;
import twitter4j.UserList;

import com.teamboid.twitter.columns.FavoritesFragment;
import com.teamboid.twitter.columns.MediaTimelineFragment;
import com.teamboid.twitter.columns.MentionsFragment;
import com.teamboid.twitter.columns.MessagesFragment;
import com.teamboid.twitter.columns.MyListsFragment;
import com.teamboid.twitter.columns.NearbyFragment;
import com.teamboid.twitter.columns.ProfileTimelineFragment;
import com.teamboid.twitter.columns.SavedSearchFragment;
import com.teamboid.twitter.columns.TimelineFragment;
import com.teamboid.twitter.columns.TrendsFragment;
import com.teamboid.twitter.columns.UserListFragment;
import com.teamboid.twitter.services.AccountService;
import com.teamboid.twitter.utilities.Utilities;
import com.teamboid.twitter.views.DragSortListView;
import com.teamboid.twitter.views.DragSortListView.DropListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ColumnManager extends Activity {

	private ArrayAdapter<String> adapt;
	private int selIndex;
	ArrayList<String> cols;
	private int lastTheme;
	
	private DropListener dropListen = new DropListener() {
		@Override
		public void drop(int from, int to) {
			String toMoveRaw = cols.get(from);
			removeColumn(from);
			cols.remove(from);
			cols.add(to, toMoveRaw);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			prefs.edit().putString(Long.toString(AccountService.getCurrentAccount().getId()) +
					"_columns", Utilities.arrayToJson(ColumnManager.this, cols)).commit();
			loadColumns();
		}
	};
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
	    	if(savedInstanceState.containsKey("lastTheme")) {
	    		lastTheme = savedInstanceState.getInt("lastTheme");
	    		setTheme(lastTheme);
	    	} else setTheme(Utilities.getTheme(getApplicationContext()));
	    }  else setTheme(Utilities.getTheme(getApplicationContext()));
		super.onCreate(savedInstanceState);
		setContentView(R.layout.column_manager);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		adapt = new ArrayAdapter<String>(this, R.layout.drag_list_item, R.id.text);
		final DragSortListView list = (DragSortListView)findViewById(android.R.id.list);
		list.setDropListener(dropListen);
		list.setAdapter(adapt);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("lastTheme", lastTheme);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(lastTheme == 0) lastTheme = Utilities.getTheme(getApplicationContext());
		else if(lastTheme != Utilities.getTheme(getApplicationContext())) {
			lastTheme = Utilities.getTheme(getApplicationContext());
			recreate();
			return;
		}
		loadColumns();
	}
	
	private void loadColumns() {
		adapt.clear();
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		cols = Utilities.jsonToArray(this, prefs.getString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", "")); 
		for(String c : cols) {
			if(c.equals(TimelineFragment.ID)) {
				adapt.add(getString(R.string.timeline_str));
			} else if(c.equals(MentionsFragment.ID)) {
				adapt.add(getString(R.string.mentions_str));
			} else if(c.equals(MessagesFragment.ID)) {
				adapt.add(getString(R.string.messages_str));
			} else if(c.equals(TrendsFragment.ID)) {
				adapt.add(getString(R.string.trends_str));
			} else if(c.equals(FavoritesFragment.ID)) {
				adapt.add(getString(R.string.favorites_str));
			} else if(c.startsWith(SavedSearchFragment.ID + "@")) {
				adapt.add(getString(R.string.savedsearch_str));
			} else if(c.startsWith(UserListFragment.ID + "@")) {
				adapt.add(getString(R.string.userlist_str));
			} else if(c.equals(NearbyFragment.ID)) {
				adapt.add(getString(R.string.nearby_str));
			} else if(c.equals(MediaTimelineFragment.ID)) {
				adapt.add(getString(R.string.media_timeline_str));
			} else if(c.equals(MyListsFragment.ID)) {
				adapt.add(getString(R.string.my_lists_str));
			} else if(c.startsWith(ProfileTimelineFragment.ID + "@")) {
				adapt.add(getString(R.string.user_feed_str));
			}
		}
		adapt.notifyDataSetChanged();
	}
	
	private void addColumn(String id) {
		if(AccountService.getAccounts().size() == 0) return;
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		ArrayList<String> cols = Utilities.jsonToArray(this, prefs.getString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", ""));
		cols.add(id);
		prefs.edit().putString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", Utilities.arrayToJson(this, cols)).commit();
		loadColumns();
		selIndex = getIntent().getIntExtra("tab_count", 4) - 1;
	}
	
	private void removeColumn(int index) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		final String prefName = Long.toString(AccountService.getCurrentAccount().getId()) + "_default_column";
		int beforeDefCol = prefs.getInt(prefName, 0);
		if(beforeDefCol > 0) prefs.edit().putInt(prefName, beforeDefCol - 1).commit();
		ArrayList<String> cols = Utilities.jsonToArray(this, prefs.getString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", ""));
		cols.remove(index);
		prefs.edit().putString(Long.toString(AccountService.getCurrentAccount().getId()) + "_columns", Utilities.arrayToJson(this, cols)).commit();
		int postIndex = index - 1;
		if(postIndex < 0) postIndex = 0;
		selIndex = postIndex;
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.main_actionbar, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			super.onBackPressed();
			return true;
		case R.id.doneAction:
			setResult(RESULT_OK, new Intent().putExtra("restart", true).putExtra("sel_index", selIndex));
			finish();
			return true;
		case R.id.addTimelineColAction:
			addColumn(TimelineFragment.ID);
			return true;
		case R.id.addMentionsColAction:
			addColumn(MentionsFragment.ID);
			return true;
		case R.id.addMessagesColAction:
			addColumn(MessagesFragment.ID);
			return true;
		case R.id.addTrendsColAction:
			addColumn(TrendsFragment.ID);
			return true;
		case R.id.addNearbyColAction:
			addColumn(NearbyFragment.ID);
			return true;
		case R.id.addMediaColAction:
			addColumn(MediaTimelineFragment.ID);
			return true;
		case R.id.addSavedSearchColAction:		
			Toast.makeText(getApplicationContext(), getString(R.string.loading_savedsearches), Toast.LENGTH_SHORT).show();
			new Thread(new Runnable() {
				public void run() {
					Account acc = AccountService.getCurrentAccount();
					try {
						final ResponseList<SavedSearch> lists = acc.getClient().getSavedSearches();
						runOnUiThread(new Runnable() {
							public void run() { showSavedSearchColumnAdd(lists.toArray(new SavedSearch[0])); }
						});
					} catch (TwitterException e) {
						e.printStackTrace();
						runOnUiThread(new Runnable() {
							public void run() { showSavedSearchColumnAdd(null); }
						});
					}
				}
			}).start();
			return true;
		case R.id.addFavoritesColAction:
			addColumn(FavoritesFragment.ID);
			return true;
		case R.id.addUserListColAction:
			Toast.makeText(getApplicationContext(), getString(R.string.loading_lists), Toast.LENGTH_SHORT).show();
			new Thread(new Runnable() {
				public void run() {
					Account acc = AccountService.getCurrentAccount();
					try {
						final ResponseList<UserList> lists = acc.getClient().getAllUserLists(acc.getId());
						runOnUiThread(new Runnable() {
							public void run() { showUserListColumnAdd(lists.toArray(new UserList[0])); }
						});
					} catch (TwitterException e) {
						e.printStackTrace();
						runOnUiThread(new Runnable() {
							public void run() { Toast.makeText(getApplicationContext(), getString(R.string.failed_load_lists), Toast.LENGTH_LONG).show(); }
						});
					}
				}
			}).start();
			return true;
		case R.id.addMyListsColAction:
			addColumn(MyListsFragment.ID);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void showUserListColumnAdd(final UserList[] lists) {
		if(lists == null) return;
		else if(lists.length == 0) {
			Toast.makeText(getBaseContext(), getString(R.string.no_lists), Toast.LENGTH_SHORT).show();
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIconAttribute(R.attr.cloudIcon);
		builder.setTitle(R.string.lists_str);
		ArrayList<String> items = new ArrayList<String>();
		for(UserList l : lists) items.add(l.getFullName());
		builder.setItems(items.toArray(new String[0]), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				UserList curList = lists[item];
				addColumn(UserListFragment.ID + "@" + curList.getFullName().replace("@", "%40") + "@" + Integer.toString(curList.getId()));
			}
		});
		builder.create().show();
	}

	private void showSavedSearchColumnAdd(final SavedSearch[] lists) {
		final Dialog diag = new Dialog(this);
		diag.setTitle(R.string.savedsearch_str);
		diag.setCancelable(true);
		diag.setContentView(R.layout.savedsearch_dialog);
		ArrayList<String> items = new ArrayList<String>();
		for(SavedSearch l : lists) items.add(l.getName());
		final ListView list = (ListView)diag.findViewById(android.R.id.list); 
		list.setAdapter(new ArrayAdapter<String>(this, R.layout.trends_list_item, items));
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index, long id) {
				SavedSearch curList = lists[index];
				addColumn(SavedSearchFragment.ID + "@" + curList.getQuery().replace("@", "%40"));
				diag.dismiss();
			}
		});
		list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int index, long id) {
				Toast.makeText(ColumnManager.this, R.string.swipe_to_delete_items, Toast.LENGTH_LONG).show();
				return false;
			}
		});
		final EditText input = (EditText)diag.findViewById(android.R.id.input);
		input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_GO) {
					final String query = input.getText().toString().trim();
					diag.dismiss();
					addColumn(SavedSearchFragment.ID + "@" + query.replace("@", "%40"));
					new Thread(new Runnable() {
						public void run() {
							try { AccountService.getCurrentAccount().getClient().createSavedSearch(query); }
							catch(Exception e) {
								e.printStackTrace();
								runOnUiThread(new Runnable() {
									public void run() { Toast.makeText(getApplicationContext(), R.string.savedsearch_upload_error, Toast.LENGTH_SHORT).show(); }
								});
								return;
							}
							runOnUiThread(new Runnable() {
								public void run() { Toast.makeText(getApplicationContext(), R.string.savedsearch_uploaded, Toast.LENGTH_SHORT).show(); }
							});
						}
					}).start();
				}
				return false;
			}
		});
		diag.show();
	}
}
