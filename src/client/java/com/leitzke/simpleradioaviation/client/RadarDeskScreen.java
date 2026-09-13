package com.leitzke.simpleradioaviation.client;

import com.leitzke.simpleradioaviation.block.AviationRadarBlock;
import com.leitzke.simpleradioaviation.network.RadarContactsPayload;
import com.leitzke.simpleradioaviation.network.RadarContactsRequestPayload;
import com.leitzke.simpleradioaviation.network.RadarSwitchPayload;
import com.leitzke.simpleradioaviation.network.DeskRunwayPayload;
import com.leitzke.simpleradioaviation.radar.RadarContact;
import com.leitzke.simpleradioaviation.radar.RunwayInfo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class RadarDeskScreen extends Screen {
    // While this screen is open, sample at the same 20 Hz cadence as the server
    // rotor. The previous 10-tick interval let the line travel roughly 100 degrees
    // before a swept contact became visible.
    private static final int REQUEST_INTERVAL_TICKS = 1;
    private static final double MAX_SWEEP_EXTRAPOLATION_TICKS = 40.0D;
    private static final int PANEL_BACKGROUND = 0xE6101418;
    private static final int RADAR_BACKGROUND = 0xF008130D;
    private static final int RADAR_GRID = 0xFF1C713A;
    private static final int BEARING_LABEL = 0xFF17562D;
    private static final int LABEL_BACKGROUND = 0xB0000000;
    private static final int HOVERED_LABEL_BACKGROUND = 0xE8000000;
    private static final int[] RUNWAY_FILL_COLORS = {
            0x44FF4545, 0x44FFD84A, 0x44558CFF, 0x44FF62C8
    };
    private static final int[] RUNWAY_OUTLINE_COLORS = {
            0xCCFF5555, 0xCCFFE067, 0xCC69A0FF, 0xCCFF7AD2
    };
    private static final double MIN_VIEW_RANGE = 250.0D;
    private static final double MAX_VIEW_RANGE = AviationRadarBlock.HORIZONTAL_RANGE;
    private static final double ZOOM_STEP = 1.25D;
    private static final Map<BlockPos, Double> SAVED_VIEW_RANGES = new HashMap<>();

    private final BlockPos deskPosition;
    private RadarContactsPayload.Status status;
    @Nullable
    private BlockPos radarPosition;
    private List<RadarContact> contacts = List.of();
    private List<ContactPlacement> placements = List.of();
    private List<RunwayInfo> runways = List.of();
    private List<RunwayPlacement> runwayPlacements = List.of();
    private final Map<ResourceLocation, Optional<String>> vehicleNameCache = new HashMap<>();
    @Nullable
    private LayoutKey placementLayout;
    private boolean placementsDirty = true;
    @Nullable
    private UUID selectedContactId;
    @Nullable
    private UUID hoveredContactId;
    private int requestCountdown;
    private float sweepHeading;
    private float sweepSpeed;
    private long sweepSampleNanos;
    private double viewRange;
    @Nullable
    private RadarViewport radarViewport;
    @Nullable private Rect switchButton;
    @Nullable private Rect addRunwayButton;
    @Nullable private Rect removeRunwayButton;
    private List<RunwayRow> runwayRows = List.of();
    @Nullable private String hoveredRunwayCode;
    @Nullable private String selectedRunwayCode;
    private String radarName = "";
    private int radarIndex;
    private int radarCount;

    public RadarDeskScreen(BlockPos deskPosition) {
        super(Component.translatable("interface.simpleradio_aviation.radar.title"));
        this.deskPosition = deskPosition.immutable();
        this.viewRange = SAVED_VIEW_RANGES.getOrDefault(this.deskPosition, MAX_VIEW_RANGE);
    }

    public boolean accepts(RadarContactsPayload payload) {
        return deskPosition.equals(payload.deskPosition());
    }

    public void updateContacts(RadarContactsPayload payload) {
        float locallyDisplayedHeading = getDisplayedSweepHeading();
        boolean hadSweepSample = sweepSampleNanos != 0L;
        boolean sameActiveRadar = status == RadarContactsPayload.Status.ACTIVE
                && payload.status() == RadarContactsPayload.Status.ACTIVE
                && Objects.equals(radarPosition, payload.radarPosition());

        status = payload.status();
        radarPosition = payload.radarPosition();
        radarName = payload.radarName();
        radarIndex = payload.radarIndex();
        radarCount = payload.radarCount();
        contacts = payload.contacts();
        runways = payload.runways();
        float authoritativeHeading = payload.sweepHeading();
        float headingError = Math.abs(Mth.wrapDegrees(
                authoritativeHeading - locallyDisplayedHeading));
        // Frequent packets arrive slightly behind the locally interpolated line.
        // Keep the continuous position during normal operation and only snap when
        // opening/switching radar or after a genuine large desynchronization.
        sweepHeading = hadSweepSample && sameActiveRadar && headingError < 90.0F
                ? locallyDisplayedHeading
                : authoritativeHeading;
        sweepSpeed = payload.sweepSpeed();
        sweepSampleNanos = System.nanoTime();
        placementsDirty = true;

        if (selectedContactId != null
                && contacts.stream().noneMatch(
                contact -> contact.playerId().equals(selectedContactId))) {
            selectedContactId = null;
        }
        if (selectedRunwayCode != null && runways.stream().noneMatch(
                runway -> runway.code().equals(selectedRunwayCode))) selectedRunwayCode = null;
    }

    @Override
    protected void init() {
        super.init();
        requestContacts();
    }

    @Override
    public void tick() {
        super.tick();
        if (requestCountdown <= 0) {
            requestContacts();
        } else {
            requestCountdown--;
        }
    }

    private void requestContacts() {
        requestCountdown = REQUEST_INTERVAL_TICKS - 1;
        if (minecraft != null
                && minecraft.getConnection() != null
                && ClientPlayNetworking.canSend(RadarContactsRequestPayload.TYPE)) {
            ClientPlayNetworking.send(new RadarContactsRequestPayload(deskPosition));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen#renderBackground applies Minecraft's menu blur. The radar keeps
        // the world sharp and uses only a light darkening behind its own panel.
        graphics.fill(0, 0, width, height, 0x66000000);

        int outerMargin = Math.max(8, Math.min(width, height) / 40);
        int panelX = outerMargin;
        int panelY = outerMargin;
        int panelWidth = width - outerMargin * 2;
        int panelHeight = height - outerMargin * 2;
        int sideWidth = Mth.clamp(panelWidth / 4, 120, 300);
        sideWidth = Math.min(sideWidth, Math.max(80, panelWidth / 2));
        int dividerX = panelX + panelWidth - sideWidth;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight,
                PANEL_BACKGROUND);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0xFF65706E);
        graphics.fill(dividerX, panelY, dividerX + 1, panelY + panelHeight,
                0xFF65706E);

        graphics.drawString(font, title, panelX + 10, panelY + 9,
                0xFFE6ECE8, false);

        int radarAreaX = panelX + 10;
        int radarAreaY = panelY + 28;
        int radarAreaWidth = Math.max(1, dividerX - radarAreaX - 10);
        int radarAreaHeight = Math.max(1, panelY + panelHeight - radarAreaY - 10);
        int radarDiameter = Math.max(40, Math.min(radarAreaWidth, radarAreaHeight) - 8);
        int radarRadius = radarDiameter / 2;
        int centerX = radarAreaX + radarAreaWidth / 2;
        int centerY = radarAreaY + radarAreaHeight / 2;
        radarViewport = new RadarViewport(centerX, centerY, radarRadius);

        drawRadar(graphics, centerX, centerY, radarRadius);
        runwayPlacements = createRunwayPlacements(centerX, centerY, radarRadius);
        hoveredRunwayCode = findHoveredRunway(mouseX, mouseY, centerX, centerY, radarRadius);
        drawRunways(graphics, centerX, centerY, radarRadius);
        drawSweep(graphics, centerX, centerY, radarRadius);
        LayoutKey layout = new LayoutKey(centerX, centerY, radarRadius, radarAreaX,
                radarAreaY, radarAreaWidth, radarAreaHeight, viewRange);
        if (placementsDirty || !layout.equals(placementLayout)) {
            placements = createPlacements(centerX, centerY, radarRadius, radarAreaX,
                    radarAreaY, radarAreaWidth, radarAreaHeight);
            placementLayout = layout;
            placementsDirty = false;
        }
        hoveredContactId = findHoveredContact(mouseX, mouseY);
        drawContacts(graphics, centerX, centerY, radarRadius);

        RadarContact focusedContact = findContact(
                hoveredContactId != null ? hoveredContactId : selectedContactId
        );
        switchButton = null;
        drawSidePanel(graphics, dividerX + 10, panelY + 30,
                sideWidth - 20, focusedContact);
        RunwayInfo focusedRunway = findRunway(
                hoveredRunwayCode != null ? hoveredRunwayCode : selectedRunwayCode);
        drawRunwayPanel(graphics, dividerX + 10, panelY + panelHeight - 10,
                sideWidth - 20, focusedRunway);

        // There are no vanilla widgets on this screen. Calling Screen#render here
        // would render its background after the radar and blur the finished HUD.
    }

    private void drawRadar(GuiGraphics graphics, int centerX, int centerY, int radius) {
        graphics.fill(centerX - radius, centerY - radius,
                centerX + radius + 1, centerY + radius + 1, RADAR_BACKGROUND);

        double scale = radius / viewRange;
        int radarOriginX = centerX;
        int radarOriginY = centerY;

        for (int ring = 1; ring <= 4; ring++) {
            drawCircleClipped(graphics, radarOriginX, radarOriginY,
                    radius * ring / 4, centerX, centerY, radius, RADAR_GRID);
        }
        drawLineClipped(graphics, centerX - radius, radarOriginY,
                centerX + radius, radarOriginY, centerX, centerY, radius, 0xAA1C713A);
        drawLineClipped(graphics, radarOriginX, centerY - radius,
                radarOriginX, centerY + radius, centerX, centerY, radius, 0xAA1C713A);
        drawBearingLabels(graphics, centerX, centerY, radius);

        drawCircle(graphics, centerX, centerY, radius, 0xFF45C96A);
        graphics.drawCenteredString(font, "N", centerX, centerY - radius + 4,
                0xFF8CFF9F);
        Component rangeText = Component.translatable(
                "interface.simpleradio_aviation.radar.range",
                String.format(Locale.ROOT, "%.0f", viewRange)
        );
        graphics.drawString(font, rangeText, centerX - radius + 6,
                centerY + radius - 12, 0xFF8CFF9F, false);
        Component controls = Component.translatable(
                "interface.simpleradio_aviation.radar.controls"
        );
        graphics.drawString(font, controls,
                centerX + radius - 6 - font.width(controls),
                centerY + radius - 12, 0xFF8DA098, false);
    }

    private void drawSweep(GuiGraphics graphics, int centerX, int centerY, int radius) {
        if (status != RadarContactsPayload.Status.ACTIVE) return;
        double sweepAngle = Math.toRadians(getDisplayedSweepHeading());
        drawRayInsideCircle(graphics, centerX, centerY,
                Math.sin(sweepAngle), -Math.cos(sweepAngle),
                centerX, centerY, radius, 0xCC45FF79);
    }

    private List<RunwayPlacement> createRunwayPlacements(int centerX, int centerY, int radius) {
        if (radarPosition == null) return List.of();
        double scale = radius / viewRange;
        List<RunwayPlacement> result = new ArrayList<>(runways.size());
        for (int index = 0; index < runways.size(); index++) {
            RunwayInfo runway = runways.get(index);
            if (!runway.complete()) continue;
            BlockPos first = runway.firstPosition();
            BlockPos second = runway.secondPosition();
            double firstX = centerX + (first.getX() - radarPosition.getX()) * scale;
            double firstY = centerY + (first.getZ() - radarPosition.getZ()) * scale;
            double secondX = centerX + (second.getX() - radarPosition.getX()) * scale;
            double secondY = centerY + (second.getZ() - radarPosition.getZ()) * scale;
            double deltaX = secondX - firstX;
            double deltaY = secondY - firstY;
            double length = Math.hypot(deltaX, deltaY);
            if (length < 0.001D) continue;
            double halfWidth = Math.max(1.0D, runway.width() * scale * 0.5D);
            double normalX = -deltaY / length * halfWidth;
            double normalY = deltaX / length * halfWidth;
            result.add(new RunwayPlacement(runway, index,
                    new double[]{firstX + normalX, secondX + normalX,
                            secondX - normalX, firstX - normalX},
                    new double[]{firstY + normalY, secondY + normalY,
                            secondY - normalY, firstY - normalY},
                    firstX, firstY, secondX, secondY));
        }
        return List.copyOf(result);
    }

    private void drawRunways(GuiGraphics graphics, int centerX, int centerY, int radius) {
        for (RunwayPlacement placement : runwayPlacements) {
            int palette = placement.index() % RUNWAY_FILL_COLORS.length;
            fillPolygonClipped(graphics, placement.xPoints(), placement.yPoints(),
                    centerX, centerY, radius, RUNWAY_FILL_COLORS[palette]);
            int outline = placement.runway().code().equals(hoveredRunwayCode)
                    ? 0xFFFFFFFF : RUNWAY_OUTLINE_COLORS[palette];
            for (int edge = 0; edge < 4; edge++) {
                int next = (edge + 1) % 4;
                drawLineClipped(graphics,
                        (int) Math.round(placement.xPoints()[edge]),
                        (int) Math.round(placement.yPoints()[edge]),
                        (int) Math.round(placement.xPoints()[next]),
                        (int) Math.round(placement.yPoints()[next]),
                        centerX, centerY, radius, outline);
            }
            drawLineClipped(graphics, (int) Math.round(placement.firstX()),
                    (int) Math.round(placement.firstY()),
                    (int) Math.round(placement.secondX()),
                    (int) Math.round(placement.secondY()),
                    centerX, centerY, radius, RUNWAY_OUTLINE_COLORS[palette]);
        }
    }

    @Nullable
    private String findHoveredRunway(double mouseX, double mouseY,
                                     int centerX, int centerY, int radius) {
        double centerDeltaX = mouseX - centerX;
        double centerDeltaY = mouseY - centerY;
        if (centerDeltaX * centerDeltaX + centerDeltaY * centerDeltaY > (double) radius * radius) {
            return null;
        }
        for (int index = runwayPlacements.size() - 1; index >= 0; index--) {
            RunwayPlacement placement = runwayPlacements.get(index);
            if (pointInsidePolygon(mouseX, mouseY, placement.xPoints(), placement.yPoints())) {
                return placement.runway().code();
            }
        }
        return null;
    }

    private static boolean pointInsidePolygon(double x, double y, double[] xs, double[] ys) {
        boolean inside = false;
        for (int current = 0, previous = xs.length - 1; current < xs.length;
             previous = current++) {
            if ((ys[current] > y) != (ys[previous] > y)
                    && x < (xs[previous] - xs[current]) * (y - ys[current])
                    / (ys[previous] - ys[current]) + xs[current]) inside = !inside;
        }
        return inside;
    }

    private static void fillPolygonClipped(GuiGraphics graphics, double[] xs, double[] ys,
                                           int clipX, int clipY, int clipRadius, int color) {
        int minimumY = Math.max(clipY - clipRadius,
                (int) Math.floor(java.util.Arrays.stream(ys).min().orElse(0.0D)));
        int maximumY = Math.min(clipY + clipRadius,
                (int) Math.ceil(java.util.Arrays.stream(ys).max().orElse(0.0D)));
        double[] intersections = new double[xs.length];
        for (int y = minimumY; y <= maximumY; y++) {
            double scanY = y + 0.5D;
            int count = 0;
            for (int edge = 0; edge < xs.length; edge++) {
                int next = (edge + 1) % xs.length;
                if ((ys[edge] > scanY) == (ys[next] > scanY)) continue;
                intersections[count++] = xs[edge] + (scanY - ys[edge])
                        * (xs[next] - xs[edge]) / (ys[next] - ys[edge]);
            }
            java.util.Arrays.sort(intersections, 0, count);
            double circleY = scanY - clipY;
            double circleHalfWidth = Math.sqrt(Math.max(0.0D,
                    (double) clipRadius * clipRadius - circleY * circleY));
            for (int pair = 0; pair + 1 < count; pair += 2) {
                int start = (int) Math.ceil(Math.max(intersections[pair], clipX - circleHalfWidth));
                int end = (int) Math.floor(Math.min(intersections[pair + 1], clipX + circleHalfWidth));
                if (end >= start) graphics.fill(start, y, end + 1, y + 1, color);
            }
        }
    }

    private void drawBearingLabels(GuiGraphics graphics, int centerX, int centerY, int radius) {
        int labelRadius = Math.max(12, radius - 19);
        for (int bearing = 0; bearing < 360; bearing += 10) {
            double radians = Math.toRadians(bearing);
            String label = String.format(Locale.ROOT, "%02d", bearing / 10);
            int labelX = centerX + (int) Math.round(Math.sin(radians) * labelRadius)
                    - font.width(label) / 2;
            int labelY = centerY - (int) Math.round(Math.cos(radians) * labelRadius)
                    - font.lineHeight / 2;
            if (bearing == 0) {
                int topY = centerY - radius + 14;
                graphics.drawCenteredString(font, "36", centerX, topY, BEARING_LABEL);
                graphics.drawCenteredString(font, "00", centerX,
                        topY + font.lineHeight, BEARING_LABEL);
                continue;
            }
            graphics.drawString(font, label, labelX, labelY, BEARING_LABEL, false);
        }
    }

    private float getDisplayedSweepHeading() {
        if (sweepSampleNanos == 0L) {
            return sweepHeading;
        }

        double elapsedTicks = (System.nanoTime() - sweepSampleNanos) / 50_000_000.0D;
        elapsedTicks = Math.min(elapsedTicks, MAX_SWEEP_EXTRAPOLATION_TICKS);
        float heading = (float) (sweepHeading + sweepSpeed * elapsedTicks);
        heading %= 360.0F;
        return heading < 0.0F ? heading + 360.0F : heading;
    }

    private List<ContactPlacement> createPlacements(int centerX, int centerY, int radius,
                                                     int areaX, int areaY,
                                                     int areaWidth, int areaHeight) {
        if (status != RadarContactsPayload.Status.ACTIVE || radarPosition == null) {
            return List.of();
        }

        List<RadarContact> sortedContacts = contacts.stream()
                .sorted(Comparator.comparing(RadarContact::playerName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<Rect> occupied = new ArrayList<>();
        List<ContactPlacement> result = new ArrayList<>(sortedContacts.size());

        for (RadarContact contact : sortedContacts) {
            double relativeX = contact.x() - (radarPosition.getX() + 0.5D);
            double relativeZ = contact.z() - (radarPosition.getZ() + 0.5D);
            double scale = radius / viewRange;
            int pointX = centerX + (int) Math.round(relativeX * scale);
            int pointY = centerY + (int) Math.round(relativeZ * scale);
            int pointDeltaX = pointX - centerX;
            int pointDeltaY = pointY - centerY;
            if (pointDeltaX * pointDeltaX + pointDeltaY * pointDeltaY > radius * radius) {
                continue;
            }
            String vehicleName = resolveVehicleName(contact.vehicleTypeId());
            int labelWidth = Math.max(
                    font.width(contact.playerName()),
                    vehicleName == null ? 0 : font.width(vehicleName)
            ) + 6;
            int labelHeight = vehicleName == null ? 13 : 23;
            Rect label = findFreeLabel(pointX, pointY, labelWidth, labelHeight,
                    areaX, areaY, areaWidth, areaHeight, occupied);

            if (label != null) {
                occupied.add(label);
            }
            result.add(new ContactPlacement(contact, pointX, pointY, vehicleName, label));
        }

        return List.copyOf(result);
    }

    @Nullable
    private static Rect findFreeLabel(int pointX, int pointY, int width, int height,
                                      int areaX, int areaY, int areaWidth, int areaHeight,
                                      List<Rect> occupied) {
        int[][] offsets = {
                {-width / 2, -height - 5}, {6, -height / 2},
                {-width - 6, -height / 2}, {-width / 2, 6},
                {-width / 2, -height - 18}, {10, -height - 10},
                {-width - 10, -height - 10}, {10, 8}, {-width - 10, 8}
        };

        for (int[] offset : offsets) {
            Rect candidate = new Rect(pointX + offset[0], pointY + offset[1], width, height);
            if (candidate.x() < areaX
                    || candidate.y() < areaY
                    || candidate.right() > areaX + areaWidth
                    || candidate.bottom() > areaY + areaHeight
                    || occupied.stream().anyMatch(candidate::intersects)) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    @Nullable
    private UUID findHoveredContact(int mouseX, int mouseY) {
        for (int index = placements.size() - 1; index >= 0; index--) {
            ContactPlacement placement = placements.get(index);
            if (placement.label() != null && placement.label().contains(mouseX, mouseY)) {
                return placement.contact().playerId();
            }
        }

        return placements.stream()
                .filter(placement -> {
                    int deltaX = mouseX - placement.pointX();
                    int deltaY = mouseY - placement.pointY();
                    return deltaX * deltaX + deltaY * deltaY <= 25;
                })
                .min(Comparator.comparingDouble(placement -> {
                    int deltaX = mouseX - placement.pointX();
                    int deltaY = mouseY - placement.pointY();
                    return deltaX * deltaX + deltaY * deltaY;
                }))
                .map(placement -> placement.contact().playerId())
                .orElse(null);
    }

    private void drawContacts(GuiGraphics graphics, int centerX, int centerY, int radius) {
        for (ContactPlacement placement : placements) {
            boolean hovered = placement.contact().playerId().equals(hoveredContactId);
            boolean selected = placement.contact().playerId().equals(selectedContactId);
            int pointColor = hovered ? 0xFFFFFF55 : selected ? 0xFF55DDFF : 0xFF4DFF78;
            graphics.fill(placement.pointX() - 2, placement.pointY() - 2,
                    placement.pointX() + 3, placement.pointY() + 3, pointColor);

            if (!hovered && placement.label() != null) {
                drawContactLabel(graphics, placement, placement.label(), false);
            }
        }

        if (hoveredContactId == null) {
            return;
        }

        ContactPlacement hovered = placements.stream()
                .filter(placement -> placement.contact().playerId().equals(hoveredContactId))
                .findFirst()
                .orElse(null);
        if (hovered == null) {
            return;
        }

        Rect label = hovered.label();
        if (label == null) {
            int labelWidth = Math.max(
                    font.width(hovered.contact().playerName()),
                    hovered.vehicleName() == null ? 0 : font.width(hovered.vehicleName())
            ) + 6;
            int labelHeight = hovered.vehicleName() == null ? 13 : 23;
            label = new Rect(
                    Mth.clamp(hovered.pointX() - labelWidth / 2,
                            centerX - radius, centerX + radius - labelWidth),
                    Mth.clamp(hovered.pointY() - labelHeight - 5,
                            centerY - radius, centerY + radius - labelHeight),
                    labelWidth,
                    labelHeight
            );
        }
        drawContactLabel(graphics, hovered, label, true);
    }

    private void drawContactLabel(GuiGraphics graphics, ContactPlacement placement,
                                  Rect rect, boolean hovered) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(),
                hovered ? HOVERED_LABEL_BACKGROUND : LABEL_BACKGROUND);
        graphics.drawString(font, placement.contact().playerName(),
                rect.x() + 3, rect.y() + 2, 0xFFFFFFFF, false);
        if (placement.vehicleName() != null) {
            graphics.drawString(font, placement.vehicleName(),
                    rect.x() + 3, rect.y() + 12, 0xFFB8C4BE, false);
        }
    }

    private void drawSidePanel(GuiGraphics graphics, int x, int y, int width,
                               @Nullable RadarContact contact) {
        if (radarCount > 1) {
            switchButton = new Rect(x, y, width, 22);
            graphics.fill(x, y, x + width, y + 22, 0xFF223129);
            graphics.renderOutline(x, y, width, 22, 0xFF5CBF78);
            graphics.drawCenteredString(font, Component.translatable(
                    "interface.simpleradio_aviation.radar.switch", radarIndex + 1, radarCount, radarName),
                    x + width / 2, y + 7, 0xFFE6ECE8);
            y += 32;
        }
        graphics.drawString(font,
                Component.translatable("interface.simpleradio_aviation.radar.details"),
                x, y, 0xFFE6ECE8, false);
        y += 18;

        if (status == null) {
            drawWrapped(graphics,
                    Component.translatable("interface.simpleradio_aviation.radar.loading"),
                    x, y, width, 0xFFB8C4BE);
            return;
        }
        if (status == RadarContactsPayload.Status.DISCONNECTED) {
            drawWrapped(graphics,
                    Component.translatable("interface.simpleradio_aviation.radar.disconnected"),
                    x, y, width, 0xFFFF8D78);
            return;
        }
        if (status == RadarContactsPayload.Status.RADAR_OFFLINE) {
            drawWrapped(graphics,
                    Component.translatable("interface.simpleradio_aviation.radar.offline"),
                    x, y, width, 0xFFFFC65C);
            return;
        }
        if (contact == null) {
            Component message = contacts.isEmpty()
                    ? Component.translatable("interface.simpleradio_aviation.radar.no_contacts")
                    : Component.translatable("interface.simpleradio_aviation.radar.select_contact");
            drawWrapped(graphics, message, x, y, width, 0xFFB8C4BE);
            return;
        }

        String vehicleName = resolveVehicleName(contact.vehicleTypeId());
        graphics.drawString(font, contact.playerName(), x, y, 0xFFFFFFFF, false);
        y += 14;
        if (vehicleName != null) {
            graphics.drawString(font, vehicleName, x, y, 0xFFB8C4BE, false);
            y += 16;
        }

        if (radarPosition == null) {
            return;
        }

        double distance = Math.hypot(
                contact.x() - (radarPosition.getX() + 0.5D),
                contact.z() - (radarPosition.getZ() + 0.5D)
        );
        double horizontalSpeed = Math.hypot(contact.velocityX(), contact.velocityZ()) * 20.0D;
        double verticalSpeed = contact.velocityY() * 20.0D;
        double horizontalSpeedKmh = horizontalSpeed * 3.6D;
        double verticalSpeedKmh = verticalSpeed * 3.6D;
        float heading = Mth.wrapDegrees(contact.heading() + 180.0F);
        if (heading < 0.0F) {
            heading += 360.0F;
        }

        y = drawValue(graphics, x, y,
                "interface.simpleradio_aviation.radar.distance",
                String.format(Locale.ROOT, "%.0f BLK", distance));
        y = drawValue(graphics, x, y,
                "interface.simpleradio_aviation.radar.altitude",
                String.format(Locale.ROOT, "%.0f BLK", contact.y()));
        y = drawCoordinates(graphics, x, y, contact);
        y = drawValue(graphics, x, y,
                "interface.simpleradio_aviation.radar.heading",
                formatHeading(heading) + "°");
        y = drawDualUnitValue(graphics, x, y,
                "interface.simpleradio_aviation.radar.speed",
                String.format(Locale.ROOT, "%.1f BLK/s", horizontalSpeed),
                String.format(Locale.ROOT, "%.1f km/h", horizontalSpeedKmh));
        drawDualUnitValue(graphics, x, y,
                "interface.simpleradio_aviation.radar.vertical_speed",
                String.format(Locale.ROOT, "%+.1f BLK/s", verticalSpeed),
                String.format(Locale.ROOT, "%+.1f km/h", verticalSpeedKmh));
    }

    private void drawRunwayPanel(GuiGraphics graphics, int x, int bottom, int width,
                                 @Nullable RunwayInfo focused) {
        int top = bottom - 148;
        graphics.drawString(font, Component.translatable(
                        "interface.simpleradio_aviation.runway.list", runways.size(), 4),
                x, top, 0xFFE6ECE8, false);

        List<RunwayRow> rows = new ArrayList<>(runways.size());
        int rowY = top + 14;
        for (int index = 0; index < runways.size(); index++) {
            RunwayInfo runway = runways.get(index);
            Rect row = new Rect(x, rowY, width, 11);
            rows.add(new RunwayRow(runway.code(), row));
            boolean highlighted = runway.code().equals(hoveredRunwayCode)
                    || runway.code().equals(selectedRunwayCode);
            if (highlighted) graphics.fill(row.x(), row.y(), row.right(), row.bottom(), 0x66314B3A);
            int palette = index % RUNWAY_OUTLINE_COLORS.length;
            graphics.fill(x + 1, rowY + 2, x + 7, rowY + 8, RUNWAY_OUTLINE_COLORS[palette]);
            String rowText = runway.complete() ? runway.displayName()
                    : runway.code() + " (...)";
            graphics.drawString(font, rowText, x + 11, rowY + 1,
                    runway.complete() ? 0xFFB8C4BE : 0xFFFFA85C, false);
            rowY += 12;
        }
        runwayRows = List.copyOf(rows);

        int detailsY = top + 65;
        if (focused != null) {
            graphics.drawString(font, focused.displayName(), x, detailsY, 0xFFFFFFFF, false);
            detailsY += 11;
            if (focused.complete()) {
                double length = Math.hypot(
                        focused.secondPosition().getX() - focused.firstPosition().getX(),
                        focused.secondPosition().getZ() - focused.firstPosition().getZ());
                graphics.drawString(font, Component.translatable(
                                "interface.simpleradio_aviation.runway.length_width",
                                String.format(Locale.ROOT, "%.0f", length), focused.width()),
                        x, detailsY, 0xFF8DA098, false);
                detailsY += 11;
                double firstHeading = runwayBearing(focused.firstPosition(), focused.secondPosition());
                double secondHeading = (firstHeading + 180.0D) % 360.0D;
                graphics.drawString(font, Component.translatable(
                                "interface.simpleradio_aviation.runway.headings",
                                formatHeading(firstHeading), formatHeading(secondHeading)),
                        x, detailsY, 0xFF8DA098, false);
                detailsY += 11;
                graphics.drawString(font, Component.translatable(
                                "interface.simpleradio_aviation.runway.elevations",
                                focused.firstPosition().getY(), focused.secondPosition().getY()),
                        x, detailsY, 0xFF8DA098, false);
            } else {
                graphics.drawString(font, Component.translatable(
                                "interface.simpleradio_aviation.runway.incomplete"),
                        x, detailsY, 0xFFFFA85C, false);
            }
        } else {
            graphics.drawString(font, Component.translatable(
                            "interface.simpleradio_aviation.runway.hover"),
                    x, detailsY, 0xFF8DA098, false);
        }

        int buttonY = bottom - 20;
        int gap = 4;
        int buttonWidth = (width - gap) / 2;
        addRunwayButton = new Rect(x, buttonY, buttonWidth, 18);
        removeRunwayButton = new Rect(x + buttonWidth + gap, buttonY,
                width - buttonWidth - gap, 18);
        drawPanelButton(graphics, addRunwayButton,
                Component.translatable("interface.simpleradio_aviation.runway.add"),
                runways.size() < 4);
        drawPanelButton(graphics, removeRunwayButton,
                Component.translatable("interface.simpleradio_aviation.runway.remove"),
                focused != null);
    }

    private void drawPanelButton(GuiGraphics graphics, Rect rect, Component label, boolean enabled) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(),
                enabled ? 0xFF223129 : 0xFF1A1E1C);
        graphics.renderOutline(rect.x(), rect.y(), rect.width(), rect.height(),
                enabled ? 0xFF5CBF78 : 0xFF46504A);
        graphics.drawCenteredString(font, label, rect.x() + rect.width() / 2,
                rect.y() + 5, enabled ? 0xFFE6ECE8 : 0xFF68706C);
    }

    private static double runwayBearing(BlockPos from, BlockPos to) {
        double heading = Math.toDegrees(Math.atan2(to.getX() - from.getX(),
                -(to.getZ() - from.getZ())));
        return heading < 0.0D ? heading + 360.0D : heading;
    }

    private static String formatHeading(double heading) {
        int rounded = (int) Math.round(heading) % 360;
        if (rounded < 0) rounded += 360;
        if (rounded == 0) rounded = 360;
        return String.format(Locale.ROOT, "%03d", rounded);
    }

    private int drawValue(GuiGraphics graphics, int x, int y,
                          String translationKey, String value) {
        graphics.drawString(font, Component.translatable(translationKey),
                x, y, 0xFF8DA098, false);
        y += 10;
        graphics.drawString(font, value, x, y, 0xFFE6ECE8, false);
        return y + 14;
    }

    private int drawDualUnitValue(GuiGraphics graphics, int x, int y,
                                  String translationKey, String primaryValue,
                                  String secondaryValue) {
        graphics.drawString(font, Component.translatable(translationKey),
                x, y, 0xFF8DA098, false);
        y += 10;
        graphics.drawString(font, primaryValue, x, y, 0xFFE6ECE8, false);
        graphics.drawString(font, secondaryValue,
                x + font.width(primaryValue) + 7, y, 0xFF738078, false);
        return y + 14;
    }

    private int drawCoordinates(GuiGraphics graphics, int x, int y,
                                RadarContact contact) {
        graphics.drawString(font,
                Component.translatable("interface.simpleradio_aviation.radar.coordinates"),
                x, y, 0xFF8DA098, false);
        y += 10;
        graphics.drawString(font, "X: " + Mth.floor(contact.x()),
                x, y, 0xFFE6ECE8, false);
        y += 10;
        graphics.drawString(font, "Y: " + Mth.floor(contact.y()),
                x, y, 0xFFE6ECE8, false);
        y += 10;
        graphics.drawString(font, "Z: " + Mth.floor(contact.z()),
                x, y, 0xFFE6ECE8, false);
        return y + 14;
    }

    private void drawWrapped(GuiGraphics graphics, Component text, int x, int y,
                             int width, int color) {
        graphics.drawWordWrap(font, text, x, y, width, color);
    }

    @Nullable
    private String resolveVehicleName(@Nullable ResourceLocation vehicleTypeId) {
        if (vehicleTypeId == null) {
            return null;
        }

        return vehicleNameCache.computeIfAbsent(
                vehicleTypeId,
                RadarDeskScreen::findTranslatedVehicleName
        ).orElse(null);
    }

    private static Optional<String> findTranslatedVehicleName(ResourceLocation vehicleTypeId) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(vehicleTypeId)
                .map(type -> type.getDescriptionId())
                .filter(I18n::exists)
                .map(I18n::get)
                .filter(name -> !name.isBlank());
    }

    @Nullable
    private RadarContact findContact(@Nullable UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return contacts.stream()
                .filter(contact -> contact.playerId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private RunwayInfo findRunway(@Nullable String code) {
        if (code == null) return null;
        return runways.stream().filter(runway -> runway.code().equals(code))
                .findFirst().orElse(null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (switchButton != null && switchButton.contains(mouseX, mouseY)) {
                if (ClientPlayNetworking.canSend(RadarSwitchPayload.TYPE)) {
                    ClientPlayNetworking.send(new RadarSwitchPayload(deskPosition));
                }
                return true;
            }
            if (addRunwayButton != null && addRunwayButton.contains(mouseX, mouseY)
                    && runways.size() < 4 && minecraft != null) {
                minecraft.setScreen(new RunwayDeskConfigScreen(this, deskPosition,
                        runways.stream().map(RunwayInfo::code).toList()));
                return true;
            }
            RunwayInfo focusedRunway = findRunway(
                    hoveredRunwayCode != null ? hoveredRunwayCode : selectedRunwayCode);
            if (removeRunwayButton != null && removeRunwayButton.contains(mouseX, mouseY)
                    && focusedRunway != null
                    && ClientPlayNetworking.canSend(DeskRunwayPayload.TYPE)) {
                ClientPlayNetworking.send(new DeskRunwayPayload(deskPosition,
                        DeskRunwayPayload.Action.REMOVE, focusedRunway.code()));
                selectedRunwayCode = null;
                return true;
            }
            for (RunwayRow row : runwayRows) {
                if (row.rect().contains(mouseX, mouseY)) {
                    selectedRunwayCode = row.code();
                    selectedContactId = null;
                    return true;
                }
            }
            if (hoveredRunwayCode != null) {
                selectedRunwayCode = hoveredRunwayCode;
                selectedContactId = null;
                return true;
            }
            selectedContactId = hoveredContactId;
            if (hoveredContactId != null) selectedRunwayCode = null;
            return hoveredContactId != null || super.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0.0D
                || radarViewport == null
                || !radarViewport.contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        double newRange = Mth.clamp(
                viewRange * Math.pow(ZOOM_STEP, -verticalAmount),
                MIN_VIEW_RANGE,
                MAX_VIEW_RANGE
        );
        if (Math.abs(newRange - viewRange) < 0.01D) {
            return true;
        }

        viewRange = newRange;
        SAVED_VIEW_RANGES.put(deskPosition, viewRange);
        placementsDirty = true;
        return true;
    }

    private static void drawCircle(GuiGraphics graphics, int centerX, int centerY,
                                   int radius, int color) {
        int samples = Math.max(48, radius * 3);
        for (int sample = 0; sample < samples; sample++) {
            double angle = sample * Math.PI * 2.0D / samples;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void drawCircleClipped(GuiGraphics graphics, int circleX, int circleY,
                                          int circleRadius, int clipX, int clipY,
                                          int clipRadius, int color) {
        int samples = Math.max(48, circleRadius * 3);
        long clipRadiusSquared = (long) clipRadius * clipRadius;
        for (int sample = 0; sample < samples; sample++) {
            double angle = sample * Math.PI * 2.0D / samples;
            int x = circleX + (int) Math.round(Math.cos(angle) * circleRadius);
            int y = circleY + (int) Math.round(Math.sin(angle) * circleRadius);
            long deltaX = x - clipX;
            long deltaY = y - clipY;
            if (deltaX * deltaX + deltaY * deltaY <= clipRadiusSquared) {
                graphics.fill(x, y, x + 1, y + 1, color);
            }
        }
    }

    private static void drawRayInsideCircle(GuiGraphics graphics, int originX, int originY,
                                            double directionX, double directionY,
                                            int clipX, int clipY, int clipRadius, int color) {
        double offsetX = originX - clipX;
        double offsetY = originY - clipY;
        double projection = offsetX * directionX + offsetY * directionY;
        double discriminant = projection * projection
                - (offsetX * offsetX + offsetY * offsetY - clipRadius * clipRadius);
        if (discriminant < 0.0D) {
            return;
        }

        double root = Math.sqrt(discriminant);
        double startDistance = Math.max(0.0D, -projection - root);
        double endDistance = -projection + root;
        if (endDistance < startDistance) {
            return;
        }

        int startX = (int) Math.round(originX + directionX * startDistance);
        int startY = (int) Math.round(originY + directionY * startDistance);
        int endX = (int) Math.round(originX + directionX * endDistance);
        int endY = (int) Math.round(originY + directionY * endDistance);
        drawLineClipped(graphics, startX, startY, endX, endY,
                clipX, clipY, clipRadius, color);
    }

    private static void drawLineClipped(GuiGraphics graphics, int x0, int y0, int x1, int y1,
                                        int clipX, int clipY, int clipRadius, int color) {
        int[] clipped = clipLineToSquare(x0, y0, x1, y1,
                clipX - clipRadius, clipY - clipRadius,
                clipX + clipRadius, clipY + clipRadius);
        if (clipped == null) return;
        x0 = clipped[0];
        y0 = clipped[1];
        x1 = clipped[2];
        y1 = clipped[3];

        int deltaX = Math.abs(x1 - x0);
        int stepX = x0 < x1 ? 1 : -1;
        int deltaY = -Math.abs(y1 - y0);
        int stepY = y0 < y1 ? 1 : -1;
        int error = deltaX + deltaY;
        long clipRadiusSquared = (long) clipRadius * clipRadius;

        while (true) {
            long offsetX = x0 - clipX;
            long offsetY = y0 - clipY;
            if (offsetX * offsetX + offsetY * offsetY <= clipRadiusSquared) {
                graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            }
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int doubledError = error * 2;
            if (doubledError >= deltaY) {
                error += deltaY;
                x0 += stepX;
            }
            if (doubledError <= deltaX) {
                error += deltaX;
                y0 += stepY;
            }
        }
    }

    @Nullable
    private static int[] clipLineToSquare(int x0, int y0, int x1, int y1,
                                          int minimumX, int minimumY,
                                          int maximumX, int maximumY) {
        double deltaX = (double) x1 - x0;
        double deltaY = (double) y1 - y0;
        double[] interval = {0.0D, 1.0D};
        if (!clipBoundary(-deltaX, x0 - (double) minimumX, interval)
                || !clipBoundary(deltaX, (double) maximumX - x0, interval)
                || !clipBoundary(-deltaY, y0 - (double) minimumY, interval)
                || !clipBoundary(deltaY, (double) maximumY - y0, interval)) {
            return null;
        }

        int clippedX0 = Mth.clamp((int) Math.round(x0 + interval[0] * deltaX),
                minimumX, maximumX);
        int clippedY0 = Mth.clamp((int) Math.round(y0 + interval[0] * deltaY),
                minimumY, maximumY);
        int clippedX1 = Mth.clamp((int) Math.round(x0 + interval[1] * deltaX),
                minimumX, maximumX);
        int clippedY1 = Mth.clamp((int) Math.round(y0 + interval[1] * deltaY),
                minimumY, maximumY);
        return new int[]{clippedX0, clippedY0, clippedX1, clippedY1};
    }

    private static boolean clipBoundary(double direction, double distance,
                                        double[] interval) {
        if (Math.abs(direction) < 1.0E-9D) return distance >= 0.0D;
        double ratio = distance / direction;
        if (direction < 0.0D) {
            if (ratio > interval[1]) return false;
            if (ratio > interval[0]) interval[0] = ratio;
        } else {
            if (ratio < interval[0]) return false;
            if (ratio < interval[1]) interval[1] = ratio;
        }
        return true;
    }

    private record Rect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double pointX, double pointY) {
            return pointX >= x && pointX < right() && pointY >= y && pointY < bottom();
        }

        boolean intersects(Rect other) {
            return x < other.right() && right() > other.x
                    && y < other.bottom() && bottom() > other.y;
        }
    }

    private record ContactPlacement(
            RadarContact contact,
            int pointX,
            int pointY,
            @Nullable String vehicleName,
            @Nullable Rect label
    ) {
    }

    private record RunwayPlacement(
            RunwayInfo runway,
            int index,
            double[] xPoints,
            double[] yPoints,
            double firstX,
            double firstY,
            double secondX,
            double secondY
    ) {}

    private record RunwayRow(String code, Rect rect) {}

    private record LayoutKey(
            int centerX,
            int centerY,
            int radius,
            int areaX,
            int areaY,
            int areaWidth,
            int areaHeight,
            double viewRange
    ) {
    }

    private record RadarViewport(int centerX, int centerY, int radius) {
        boolean contains(double pointX, double pointY) {
            double deltaX = pointX - centerX;
            double deltaY = pointY - centerY;
            return deltaX * deltaX + deltaY * deltaY <= (double) radius * radius;
        }
    }
}
