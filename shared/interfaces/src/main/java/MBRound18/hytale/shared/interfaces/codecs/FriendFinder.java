package MBRound18.hytale.shared.interfaces.codecs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class FriendFinder {

        // UI Buttons: @AddFriendButton, @RemoveFriendButton, @InvitePartyButton,
        // @TogglePartyStateButton

        public enum Action {
                ADD_FRIEND("AddFriend"),
                REMOVE_FRIEND("RemoveFriend"),
                INVITE_TO_PARTY("InviteToParty"),
                TOGGLE_PARTY_STATE("TogglePartyState");

                private final String wireValue;

                Action(String wireValue) {
                        this.wireValue = wireValue;
                }

                public String getWireValue() {
                        return wireValue;
                }

                public static Action fromWireValue(String value) {
                        if (value == null) {
                                return null;
                        }
                        for (Action action : values()) {
                                if (action.wireValue.equalsIgnoreCase(value)) {
                                        return action;
                                }
                        }
                        return null;
                }
        }

        public String friendSearch;
        public Action action;

        public FriendFinder() {
        }

        /**
         * Builder Codec for FriendFinder, how do we want hytale to serialzie this data
         */
        public static final BuilderCodec<FriendFinder> CODEC = BuilderCodec
                        .builder(FriendFinder.class, FriendFinder::new)
                        .append(new KeyedCodec<>("@AddFriendButton", Codec.STRING),
                                        (f, v) -> {
                                                if (v != null)
                                                        f.action = Action.fromWireValue(v);
                                        },
                                        f -> f.action == Action.ADD_FRIEND ? f.action.getWireValue() : null)
                        .add()
                        .append(new KeyedCodec<>("@RemoveFriendButton", Codec.STRING),
                                        (f, v) -> {
                                                if (v != null)
                                                        f.action = Action.fromWireValue(v);
                                        },
                                        f -> f.action == Action.REMOVE_FRIEND ? f.action.getWireValue() : null)
                        .add()
                        .append(new KeyedCodec<>("@InvitePartyButton", Codec.STRING),
                                        (f, v) -> {
                                                if (v != null)
                                                        f.action = Action.fromWireValue(v);
                                        },
                                        f -> f.action == Action.INVITE_TO_PARTY ? f.action.getWireValue() : null)
                        .add()
                        .append(new KeyedCodec<>("@TogglePartyStateButton", Codec.STRING),
                                        (f, v) -> {
                                                if (v != null)
                                                        f.action = Action.fromWireValue(v);
                                        },
                                        f -> f.action == Action.TOGGLE_PARTY_STATE ? f.action.getWireValue() : null)
                        .add()
                        .append(new KeyedCodec<>("@FriendSearch", Codec.STRING),
                                        (f, v) -> f.friendSearch = v,
                                        f -> f.friendSearch)
                        .add()
                        .build();
}
