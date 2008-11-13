package org.pmix.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.pmix.cover.CoverRetriever;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ViewSwitcher;


/**
 * Handels the events from MPDStatusMonitor
 * @author Rémi Flament, Stefan Agner
 * @version $Id:  $
 */
public class MyHandler extends Handler implements ViewSwitcher.ViewFactory {

	private MainMenuActivity mainMenuActivity = null;

	private long lastKnownElapsedTime;

	private int currentSongTime;

	private String previousAlbum = "";

	public int getCurrentSongTime() {
		return currentSongTime;
	}

	public long getLastKnownElapsedTime() {
		return lastKnownElapsedTime;
	}

	public MyHandler(MainMenuActivity mainMenuActivity) {
		this.mainMenuActivity = mainMenuActivity;
	}

	private static String timeToString(long seconds) {
		long min = seconds / 60;
		long sec = seconds - min * 60;
		return (min < 10 ? "0" + min : min) + ":" + (sec < 10 ? "0" + sec : sec);
	}

	@Override
	public void handleMessage(Message msg) {

		MPDStatus status = (MPDStatus) msg.obj;
		int songId = status.getSongPos();
		if (songId >= 0) {
			try {
				
				Music current = org.pmix.ui.Contexte.getInstance().getMpd().getPlaylist().getMusic(songId);
				if (current != null) {

					mainMenuActivity.getArtistNameText().setText(current.getArtist() != null ? current.getArtist() : "");
					mainMenuActivity.getAlbumNameText().setText((current.getAlbum() != null ? (current.getAlbum()) : ""));
					mainMenuActivity.getSongNameText().setText((current.getTitle() != null ? (current.getTitle()) : ""));

					String album = current.getAlbum();

					if (album != null && !previousAlbum.equals(album) && current.getArtist() != null) {
						String url = CoverRetriever.getCoverUrl(current.getArtist(), current.getAlbum());
						if (url != null)
						{
							// Show loading...
							//mainMenuActivity.getCoverSwitcher().setImageResource(R.drawable.gmpcnocover);
							//mainMenuActivity.getCoverSwitcher().setVisibility(ImageSwitcher.INVISIBLE);
							//mainMenuActivity.getCoverSwitcherProgress().setVisibility(ProgressBar.VISIBLE);
							new CoverDownloader(url);
						}
						else
							mainMenuActivity.getCoverSwitcher().setImageResource(R.drawable.gmpcnocover);

					}
					if (album == null) {
						mainMenuActivity.getCoverSwitcher().setImageResource(R.drawable.gmpcnocover);
					}

					previousAlbum = album;

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		lastKnownElapsedTime = status.getElapsedTime();
		if (status.getTotalTime() > 0) {
			mainMenuActivity.getTrackTime().setText(timeToString(status.getElapsedTime()) + " - " + timeToString(status.getTotalTime()));
			mainMenuActivity.getProgressBarTrack().setEnabled(true);
			mainMenuActivity.getProgressBarTrack().setProgress((int) (lastKnownElapsedTime * 100 / status.getTotalTime()));
		} else {
			mainMenuActivity.getTrackTime().setText("");
			mainMenuActivity.getProgressBarTrack().setEnabled(false);
			mainMenuActivity.getProgressBarTrack().setProgress(0);
		}
		mainMenuActivity.getProgressBar().setProgress(status.getVolume());

		currentSongTime = (int) status.getTotalTime();
	}

	public View makeView() {
		ImageView i = new ImageView(mainMenuActivity);

		i.setBackgroundColor(0x00FF0000);
		i.setScaleType(ImageView.ScaleType.FIT_CENTER);
		//i.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		
		return i;
	}

	/**
	 * Cover downloader Thread
	 * @author Stefan Agner
	 *
	 */
	class CoverDownloader extends Thread {
		private String fileUrl;
		Bitmap bmImg;
		public CoverDownloader(String fileUrl)
		{
			this.fileUrl = fileUrl;
			this.start();
		}
		
		public void run()
		{
			downloadFile(fileUrl);
		}
		void downloadFile(String fileUrl) {
			URL myFileUrl = null;
			try {
				myFileUrl = new URL(fileUrl);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
				conn.setDoInput(true);
				conn.connect();
				int length = conn.getContentLength();
	
				InputStream is = conn.getInputStream();
	
				bmImg = BitmapFactory.decodeStream(is);
				// new BitmapDrawable(bmImg);

				mainMenuActivity.runOnUiThread(new Runnable(){
//					@Override
					public void run() {
						mainMenuActivity.getCoverSwitcher().setVisibility(ImageSwitcher.VISIBLE);
						//mainMenuActivity.getCoverSwitcherProgress().setVisibility(ProgressBar.INVISIBLE);
						/*mainMenuActivity.getCurrentFocus().requestLayout();
						mainMenuActivity.getCurrentFocus().invalidate();*/
						mainMenuActivity.getCoverSwitcher().setImageDrawable(new BitmapDrawable(bmImg));
					}
				});
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
