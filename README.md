# AudioScope

##  Overview
AudioScope is a real-time audio signal visualizer built in Java. It captures live audio from the microphone, processes the waveform, and displays it dynamically using **JFreeChart**. This tool can be expanded with additional signal processing features like **filtering, FFT analysis, and equalization**

##  Features
- **Real-time audio capture** from the microphone
- **Dynamic waveform plotting** using JFreeChart
- **Ability to save raw audio data** for further processing
- **Modular design** for easy expansion (e.g., signal processing, filters, FFT, etc.)

## Installation & Setup

### **Prerequisites**
- **Java 21+**
- **Maven** 
- **JFreeChart** (`1.5.3`)

### **Clone & Build**
```sh
git clone https://github.com/Eng-Bunnys/AudioScope.git
cd AudioScope
mvn clean install
```

### **Run the Project**
```sh
mvn exec:java -Dexec.mainClass="org.bunnys.Main"
```

##  Usage
- Run the application, and it will start capturing live audio.
- The waveform will be plotted in real-time.
- Press **Ctrl+C** to stop the program.

##  Future Updates [To-Do]
✅ **Apply signal processing techniques** (filtering, FFT, equalizer)  
✅ **Add frequency spectrum visualization**  
✅ **Support saving & loading audio data**  

## License
This project is licensed under the **MIT License**.

---

### Made using Java by Bunnys 
