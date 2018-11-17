package deep.learning.human.bvh;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import deep.learning.human.humanoid.HumanoidBuilder;
import deep.learning.human.utils.config.HumanConfig;

public class BVHPreprocessor {

    public static void main(String[] args) throws IOException {
        HumanConfig preprocessed = HumanoidBuilder.build("1.bvh",
                "+__LeftShoulder",
                "+__RightShoulder",
                "!2"
/*                "+Neck1",
                "+Spine1",
                "+__LeftShoulder",
                "+__RightShoulder",
                "+RightUpLeg",
                "+LeftUpLeg",
                "-Neck"*/);
        File targetFile = new File("src/main/resources/bvh/" + "preprocessed.json");
        OutputStream outStream = new FileOutputStream(targetFile);
        new ObjectMapper().writeValue(outStream, preprocessed);
    }

}
