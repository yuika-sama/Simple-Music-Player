package com.mycompany.musicplayer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mpatric.mp3agic.Mp3File;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

public class MusicPlayer extends JFrame {
    private JLabel imageLabel;
    private final List<File> songs;
    private int cur_id = 0;

    private AdvancedPlayer player;
    private Thread playingThread;
    private Timer updateTimer;

    private JLabel startTimeLabel;
    private JLabel endTimeLabel;
    private JSlider timeSlider;
    private long currentPosition;

    private JSlider volumeSlider;
    private float currentVolume = 1.0f;

    public MusicPlayer() {
        setTitle("Music Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 450);
        setLayout(new BorderLayout());

        songs = getSongs();
        if (songs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No song has been found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 5, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;

        // Image panel
        JPanel imagePanel = new JPanel();
        imageLabel = new JLabel();
        imagePanel.add(imageLabel);
        displayImage();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1;
        mainPanel.add(imagePanel, gbc);

        JPanel songPanel = new JPanel();
        JLabel songLabel = new JLabel(songs.getFirst().getName());
        songPanel.add(songLabel);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 1;
        mainPanel.add(songPanel, gbc);

        // Time panel
        JPanel timePanel = new JPanel(new BorderLayout());
        startTimeLabel = new JLabel("00:00");
        endTimeLabel = new JLabel("99:99");
        timeSlider = getjSlider();
        startTimeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        endTimeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        timePanel.add(startTimeLabel, BorderLayout.WEST);
        timePanel.add(timeSlider, BorderLayout.CENTER);
        timePanel.add(endTimeLabel, BorderLayout.EAST);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weighty = 5;
        mainPanel.add(timePanel, gbc);

        // Button panel
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints buttonGbc = new GridBagConstraints();
        buttonGbc.insets = new Insets(5, 5, 10, 10);

        JButton btPrev = new JButton("<");
        btPrev.setPreferredSize(new Dimension(50, 50));
        buttonGbc.gridx = 0;
        buttonGbc.gridy = 4;
        btPrev.addActionListener(_ -> playPreviousSong(songLabel));
        buttonPanel.add(btPrev, buttonGbc);

        // Play button
        JButton btPlay = new JButton("►");
        btPlay.setPreferredSize(new Dimension(60, 50));
        buttonGbc.gridx = 1;
        btPlay.addActionListener(_ -> playCurrentSong(songLabel));
        buttonPanel.add(btPlay, buttonGbc);

        // Next button
        JButton btNext = new JButton(">");
        btNext.setPreferredSize(new Dimension(50, 50));
        buttonGbc.gridx = 2;
        btNext.addActionListener(_ -> playNextSong(songLabel));
        buttonPanel.add(btNext, buttonGbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel, BorderLayout.CENTER);
    }

    private static JSlider getjSlider() {
        JSlider timeSlider = new JSlider(0, 100, 0);
        timeSlider.setForeground(Color.DARK_GRAY);
        timeSlider.setPaintTicks(false);
        timeSlider.setMajorTickSpacing(10);
        timeSlider.setMinorTickSpacing(5);
        timeSlider.setPaintLabels(false);
        timeSlider.setUI(new javax.swing.plaf.metal.MetalSliderUI() {
            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.BLUE);  // Custom thumb (knob) color
                g2d.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
            }

            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.fillRoundRect(trackRect.x, trackRect.y + 5, trackRect.width, 8, 10, 10);
            }
        });
        return timeSlider;
    }

    private void displayImage() {
        try {
            BufferedImage img = ImageIO.read(new File("assets/imgs/images.png"));
            if (img != null) {
                Image scaledImage = img.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
                imageLabel.revalidate();
                imageLabel.repaint();
            } else {
                imageLabel.setIcon(null);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(this, "Image not found or can't be loaded.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static List<File> getSongs() {
        File folder = new File("assets/songs");
        List<File> songFiles = new ArrayList<>();
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((_, name) -> name.toLowerCase().endsWith(".mp3"));
            if (files != null) {
                Collections.addAll(songFiles, files);
            }
        } else {
            System.out.println("Folder does not exist");
        }
        return songFiles;
    }

    private String getCurrentSongName() {
        return songs.get(cur_id).getName();
    }

    private void playCurrentSong(JLabel songLabel) {
        stopSong();
        File songFile = songs.get(cur_id);
        songLabel.setText(getCurrentSongName());
        playSong(songFile);
        updateSongInfo(songFile);
    }

    private void updateSongInfo(File songFile) {
        try {
            Mp3File mp3file = new Mp3File(songFile);
            long duration = mp3file.getLengthInSeconds();
            endTimeLabel.setText(formatTime(duration));
            timeSlider.setMaximum((int) duration);
        } catch (Exception e) {
            System.out.println("Error reading MP3 file: " + e.getMessage());
        }

    }

    private void updatePosition() {
        if (player != null) {
            currentPosition++;
            timeSlider.setValue((int) currentPosition);
            startTimeLabel.setText(formatTime(currentPosition));

            if (currentPosition >= timeSlider.getMaximum()) {
                stopSong();
            }
        }
    }

    private String formatTime(long duration) {
        return String.format("%02d:%02d",
                TimeUnit.SECONDS.toMinutes(duration),
                duration - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(duration))
                );
    }

    private void playNextSong(JLabel songLabel) {
        cur_id = (cur_id + 1) % songs.size();
        playCurrentSong(songLabel);
    }

    private void playPreviousSong(JLabel songLabel) {
        cur_id = (cur_id - 1 + songs.size()) % songs.size();
        playCurrentSong(songLabel);
    }

    private void playSong(File songFile) {
        stopSong();
        try {
            FileInputStream fileInputStream = new FileInputStream(songFile);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            player = new AdvancedPlayer(bufferedInputStream);
            currentPosition = 0;
            player.setPlayBackListener(new PlaybackListener() {
                @Override
                public void playbackFinished(PlaybackEvent evt) {
                    SwingUtilities.invokeLater(() -> playNextSong(null));
                }
            });
            playingThread = new Thread(() -> {
                try {
                    player.play();
                } catch (JavaLayerException e) {
                    System.out.println("Error playing MP3 file: " + e.getMessage());
                }
            });
            playingThread.start();
            updateTimer = new Timer(1000, _ -> updatePosition());
            updateTimer.start();
        } catch (FileNotFoundException | JavaLayerException e) {
            System.out.println("Error initializing MP3 player: " + e.getMessage());
        }
    }

    private void stopSong() {
        if (player != null) {
            player.close();
            player = null;
        }
        if (playingThread != null) {
            playingThread.interrupt();
            playingThread = null;
        }
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MusicPlayer app = new MusicPlayer();
            app.setVisible(true);
        });
    }
}