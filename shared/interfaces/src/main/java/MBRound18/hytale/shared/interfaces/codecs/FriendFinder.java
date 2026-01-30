package MBRound18.hytale.shared.interfaces.codecs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class FriendFinder {

    public String friendSearch;

    public FriendFinder() {
    }

    /**
     * Builder Codec for FriendFinder, how do we want hytale to serialzie this data
     */
    public static final BuilderCodec<FriendFinder> CODEC = BuilderCodec.builder(FriendFinder.class, FriendFinder::new)
            .append(
                    /**
                     * Here we map the #FriendSearch from the ui file via @FriendSearch to the type
                     * of STRING
                     */
                    new KeyedCodec<>("@FriendSearch", Codec.STRING), // TLDR; key mapping
                    /**
                     * Now we say, okay the string value coming out of that gets mapped to our class
                     * field friendSearch
                     */
                    (f, v) -> f.friendSearch = v, // TLDR; setter
                    /**
                     * On the inverse to serialize it back, we take the field friendSearch and map
                     * it back to the @FriendSearch key
                     */
                    f -> f.friendSearch) // TLDR; getter
            .add()
            .build();
}
