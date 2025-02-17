package org.bunnys.audio.processing;

public class LowPassFilter implements AudioProcessor {
    // Makes this thread safe for any real time updates
    private volatile float alpha;

    public LowPassFilter(float alpha) {
        this.alpha = alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    @Override
    public float[] process(float[] input) {
        System.out.println("In LPF");
        if (input == null || input.length == 0)
            return new float[0];

        float[] filtered = new float[input.length];
        filtered[0] = input[0];

        // Print before values (limit to 5), this is added just to ensure that the LPF works
        System.out.println("Before LPF:");
        for (int i = 0; i < Math.min(5, input.length); i++) {
            System.out.print(input[i] + " ");
        }
        System.out.println();

        // Apply LPF filtering
        for (int i = 1; i < input.length; i++) {
            filtered[i] = alpha * input[i] + (1 - alpha) * filtered[i - 1];
        }

        // Print after values (limit to 5)
        System.out.println("After LPF:");
        for (int i = 0; i < Math.min(5, filtered.length); i++) {
            System.out.print(filtered[i] + " ");
        }
        System.out.println();

        return filtered;
    }
}
