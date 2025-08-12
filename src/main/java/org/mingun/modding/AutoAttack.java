package org.mingun.modding;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class AutoAttack implements ClientModInitializer {

	private KeyBinding autoAttackKeyBinding;
	private boolean isAutoAttacking = false;

	private Vec3d previousLookVector = null;

	@Override
	public void onInitializeClient() {
		autoAttackKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.autoattack.toggle",
			Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			"category.autoattack"
		));

		// 클라이언트 틱 이벤트 등록
		// 틱마다 반복
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.interactionManager == null || client.crosshairTarget == null) return;

			DoAttack(client);
			CheckViewMoved(client);
		});
	}

	private void DoAttack(MinecraftClient client) {
		// B키 눌러서 토글
		if (autoAttackKeyBinding.wasPressed()) {
			isAutoAttacking = !isAutoAttacking;
			client.player.sendMessage(
				net.minecraft.text.Text.of("Auto Attack " + (isAutoAttacking ? "Enabled" : "Disabled")),
				true
			);
		}

		if (!isAutoAttacking) return;

		HitResult target = client.crosshairTarget;

		// 쿨타임 체크
		float attackCooldown = client.player.getAttackCooldownProgress(0);
		if (attackCooldown < 1f) return;

		// 엔티티만 공격
		if (target.getType() == HitResult.Type.ENTITY) {
			EntityHitResult entityHit = (EntityHitResult) target;
			client.interactionManager.attackEntity(client.player, entityHit.getEntity());
			client.player.swingHand(client.player.getActiveHand());
		}
	}

	// 시선이 움직였으면 자동 공격 끄기
	private void CheckViewMoved(MinecraftClient client) {
		// 현재 시선 벡터 가져오기
		Vec3d currentLook = client.player.getRotationVec(1.0F);

		if (previousLookVector != null && !currentLook.equals(previousLookVector)) {
			if (isAutoAttacking) {
				client.player.sendMessage(net.minecraft.text.Text.of("Auto Attack Disabled"), true);
				isAutoAttacking = false;
			}
		}

		previousLookVector = currentLook;
	}
}