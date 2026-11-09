package openperipheral.addons.glasses.client;

import openmods.structured.IStructureContainerFactory;
import openmods.structured.IStructureElement;
import openmods.structured.StructureObserver;
import openmods.structured.StructuredDataSlave;
import openperipheral.addons.glasses.TerminalEvent;
import openperipheral.addons.glasses.drawable.Drawable;
import openperipheral.addons.glasses.drawable.DrawableType;

public abstract class DrawableContainerSlave extends StructuredDataSlave<Drawable, IStructureElement> {

    protected final long terminalId;

    private static final IStructureContainerFactory<Drawable> FACTORY = new IStructureContainerFactory<Drawable>() {

        @Override
        public Drawable createContainer(int typeId) {
            if (typeId < 0 || typeId >= DrawableType.TYPES.length)
                throw new IllegalArgumentException("Unknown drawable type id " + typeId);

            final DrawableType type = DrawableType.TYPES[typeId];
            return type.create();
        }
    };

    public DrawableContainerSlave(long terminalId, StructureObserver<Drawable, IStructureElement> observer) {
        super(FACTORY, observer);
        this.terminalId = terminalId;
    }

    public static class Private extends DrawableContainerSlave {

        public Private(long terminalId, StructureObserver<Drawable, IStructureElement> observer) {
            super(terminalId, observer);
        }

        @Override
        protected void onConsistencyCheckFail() {
            new TerminalEvent.PrivateDrawableReset(terminalId).sendToServer();
        }
    }

    public static class Public extends DrawableContainerSlave {

        public Public(long terminalId, StructureObserver<Drawable, IStructureElement> observer) {
            super(terminalId, observer);
        }

        @Override
        protected void onConsistencyCheckFail() {
            new TerminalEvent.PublicDrawableReset(terminalId).sendToServer();
        }
    }
}
