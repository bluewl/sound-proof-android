package com.example.sound_proof_android;

public class AudioSignal {

    int chunkSize, subChunk1Size, sampleRate, byteRate, subChunk2Size=1, bytePerSample;
    short audioFomart, numChannels, blockAlign, bitsPerSample=8;
    String chunkID, format, subChunk1ID, subChunk2ID;

    double[] audioData;

    public AudioSignal() {

    }

    public double[] getAudioData() {
        return audioData;
    }

    public void setAudioData(double[] audioData) {
        this.audioData = audioData;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getSubChunk1Size() {
        return subChunk1Size;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getByteRate() {
        return byteRate;
    }

    public int getSubChunk2Size() {
        return subChunk2Size;
    }

    public int getBytePerSample() {
        return bytePerSample;
    }

    public short getAudioFomart() {
        return audioFomart;
    }

    public short getNumChannels() {
        return numChannels;
    }

    public short getBlockAlign() {
        return blockAlign;
    }

    public short getBitsPerSample() {
        return bitsPerSample;
    }

    public String getChunkID() {
        return chunkID;
    }

    public String getFormat() {
        return format;
    }

    public String getSubChunk1ID() {
        return subChunk1ID;
    }

    public String getSubChunk2ID() {
        return subChunk2ID;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void setSubChunk1Size(int subChunk1Size) {
        this.subChunk1Size = subChunk1Size;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setByteRate(int byteRate) {
        this.byteRate = byteRate;
    }

    public void setSubChunk2Size(int subChunk2Size) {
        this.subChunk2Size = subChunk2Size;
    }

    public void setBytePerSample(int bytePerSample) {
        this.bytePerSample = bytePerSample;
    }

    public void setAudioFomart(short audioFomart) {
        this.audioFomart = audioFomart;
    }

    public void setNumChannels(short numChannels) {
        this.numChannels = numChannels;
    }

    public void setBlockAlign(short blockAlign) {
        this.blockAlign = blockAlign;
    }

    public void setBitsPerSample(short bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    public void setChunkID(String chunkID) {
        this.chunkID = chunkID;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setSubChunk1ID(String subChunk1ID) {
        this.subChunk1ID = subChunk1ID;
    }

    public void setSubChunk2ID(String subChunk2ID) {
        this.subChunk2ID = subChunk2ID;
    }
}
