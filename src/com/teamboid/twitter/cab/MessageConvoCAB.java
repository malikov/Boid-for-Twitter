package com.teamboid.twitter.cab;

import java.util.ArrayList;

import twitter4j.DirectMessage;
import twitter4j.TwitterException;

import android.app.Activity;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.teamboid.twitter.R;
import com.teamboid.twitter.TabsAdapter.BaseListFragment;
import com.teamboid.twitter.listadapters.MessageConvoAdapter.DMConversation;
import com.teamboid.twitter.services.AccountService;

/**
 * The contextual action bar for any lists/columns that display twitter4j.User objects.
 * @author Aidan Follestad
 */
public class MessageConvoCAB {

	public static Activity context;

	public static void clearSelectedItems() {
		for(int i = 0; i < context.getActionBar().getTabCount(); i++) {
			Fragment frag = context.getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
			if(frag instanceof BaseListFragment) {
				((BaseListFragment)frag).getListView().clearChoices();
				((BaseAdapter)((BaseListFragment)frag).getListView().getAdapter()).notifyDataSetChanged();
			}
		}
	}
	public static DMConversation[] getSelectedConvos() {
		ArrayList<DMConversation> toReturn = new ArrayList<DMConversation>();
		for(int i = 0; i < context.getActionBar().getTabCount(); i++) {
			Fragment frag = context.getFragmentManager().findFragmentByTag("page:" + Integer.toString(i));
			if(frag instanceof BaseListFragment) {
				DMConversation[] toAdd = ((BaseListFragment)frag).getSelectedMessages();
				if(toAdd != null && toAdd.length > 0) {
					for(DMConversation u : toAdd) toReturn.add(u);
				}
			}
		}
		return toReturn.toArray(new DMConversation[0]);
	}

	public static void updateTitle() {
		DMConversation[] selConvos = MessageConvoCAB.getSelectedConvos(); 
		if(selConvos.length == 1) {
			MessageConvoCAB.ConvoActionMode.setTitle(R.string.one_convo_selected);
		} else {
			MessageConvoCAB.ConvoActionMode.setTitle(context.getString(R.string.x_convos_selected).replace("{X}", Integer.toString(selConvos.length)));
		}
	}

	public static void performLongPressAction(ListView list, BaseAdapter adapt, int index) {
		if(list.isItemChecked(index)) {
			list.setItemChecked(index, false);
		} else list.setItemChecked(index, true);
		if(MessageConvoCAB.ConvoActionMode == null) {
			context.startActionMode(MessageConvoCAB.ConvoActionModeCallback);
		} else {
			final DMConversation[] convos = MessageConvoCAB.getSelectedConvos();
			MessageConvoCAB.ConvoActionMode.getMenuInflater().inflate(R.menu.convo_cab, TimelineCAB.TimelineActionMode.getMenu());
			if(convos.length == 0) {
				MessageConvoCAB.ConvoActionMode.finish();
			} else MessageConvoCAB.updateTitle();
		}

	}

	public static ActionMode ConvoActionMode;
	public static ActionMode.Callback ConvoActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MessageConvoCAB.ConvoActionMode = mode;
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.convo_cab, menu);
			updateTitle();
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }

		@Override
		public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
			item.setEnabled(false);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			item.setTitle(R.string.deleting_str);
			final DMConversation[] selConvos = getSelectedConvos();
			MessageConvoCAB.clearSelectedItems();
			switch (item.getItemId()) {
			case R.id.deleteAction:
				new Thread(new Runnable() {
					public void run() {
						for(final DMConversation convo : selConvos) {
							for(DirectMessage msg : convo.getMessages()) {
								try { AccountService.getCurrentAccount().getClient().destroyDirectMessage(msg.getId()); }
								catch (final TwitterException e) {
									e.printStackTrace();
									context.runOnUiThread(new Runnable() {
										@Override
										public void run() {
											Toast.makeText(context, context.getString(R.string.failed_delete_dm).replace("{user}", convo.getToScreenName()) + " " + e.getErrorMessage(), Toast.LENGTH_LONG).show();
										}
									});
								}
							}
							context.runOnUiThread(new Runnable() {
								public void run() {
									AccountService.getMessageConvoAdapter(context, AccountService.getCurrentAccount().getId()).remove(convo);
								}
							});
						}
						context.runOnUiThread(new Runnable() {
							public void run() { mode.finish(); }
						});
					}
				}).start();
				return true;
			default:
				return false;
			}
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			MessageConvoCAB.clearSelectedItems();
			MessageConvoCAB.ConvoActionMode = null;
		}
	};
}
