// Copyright (c) FIRST and other WPILib contributors.

// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.pathplanner.lib.util.GeometryUtil;
import com.revrobotics.CANSparkBase.IdleMode;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.math.controller.ArmFeedforward;
import frc.lib.PIDGains;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * The Constants class provides a convenient place for teams to hold robot-wide numerical or boolean
 * constants. This class should not be used for any other purpose. All constants should be declared
 * globally (i.e. public static). Do not put anything functional in this class.
 *
 * <p>It is advised to statically import this class (or one of its inner classes) wherever the
 * constants are needed, to reduce verbosity.
 */
public final class Constants {
    public static class OperatorConstants {
        public static final int DRIVER_CONTROLLER_PORT = 0;

        public static final double leftXDeadband = 0.01;
        public static final double leftYDeadband = 0.01;
        public static final double rightXDeadband = 0.01;
        public static final double rightYDeadband = 0.01;
        public static final double armManualDeadband = 0.05;
        public static final double armManualScale = 0.1;
    }

    public static class FieldConstants {

        public static final double fieldLength = Units.feetToMeters(54.27);
        public static final double fieldWidth = Units.feetToMeters(26.9375);

        public static final Translation2d blueStageCenter =
                new Translation2d(Units.inchesToMeters(156.895), fieldWidth / 2);
        public static final Translation2d redStageCenter =
                new Translation2d(fieldLength - Units.inchesToMeters(157.395), fieldWidth / 2);

        public static final double stageDangerRadius = Units.inchesToMeters(85.9 / 2 + 12)
                + SwerveConstants.PhysicalConstants.driveBaseRadius;
    }

    public static class PositionConstants {
        public static class ArmPositions {
            public static final double intake = 0.1;
        }
        public static class ShootingPositions {
            public static class ShootingPosition {
                Pose2d pose;
                double armAngle;
                double launcherSpeed;
                public ShootingPosition(Pose2d pose, double armAngle, double launcherSpeed) {
                    this.pose = pose;
                    this.armAngle = armAngle;
                    this.launcherSpeed = launcherSpeed;
                }

                public Pose2d getPose() {
                    return pose;
                }

                public double getArmAngle() {
                    return armAngle;
                }

                public double getLauncherSpeed() {
                    return launcherSpeed;
                }
            }

            /*
            Below you can declare preset shooting positions.
            Each position includes a Pose2d to represent the position and rotation of the robot to make the shot,
            an arm angle, in radians, to angle the arm to,
            and a speed to run the launcher at.

            All positions should be on the blue alliance side, with x and y values ranging from 0 to ≈8.2 meters
            and rotations usually around 0°±45°

            Any speaker scoring positions that are valid for use during teleop should be added
            to the validSpeakerShootingPositions array
             */

            public static final ShootingPosition ampScore =
                    new ShootingPosition(new Pose2d(Units.feetToMeters(4.125) // 4' 1.5" to edge from wall
                                                    + Units.feetToMeters(1), // 2 feet wide, so halfway
                            Units.feetToMeters(26.9375) // Width of field
                            - SwerveConstants.PhysicalConstants.driveBaseRadius, // Half the width of the robot
                            Rotation2d.fromDegrees(-90)),
                            1.38, 0.4);
            public static final ShootingPosition inFrontOfSpeaker =
                    new ShootingPosition(new Pose2d(0,0, Rotation2d.fromDegrees(0)), .198, 0.5);

            /** An array of all valid {@link ShootingPosition}s to score on the speaker */
            public static final ShootingPosition[] validSpeakerShootingPositions
                    = new ShootingPosition[] { // All valid positions for scoring in the Speaker should be added here
                    inFrontOfSpeaker
            };

            /**
             * Finds the nearest {@link ShootingPosition ShootingPosition} from the array of
             * {@link ShootingPositions#validSpeakerShootingPositions}
             * @param to The translation on the field to find the closest position to
             * @param onRedAllianceSide Whether to mirror the translation
             * @throws IndexOutOfBoundsException If {@link ShootingPositions#validSpeakerShootingPositions} is empty
             * @return The closest {@link ShootingPosition ShootingPosition} to the given {@link Translation2d}
             */
            public static ShootingPosition getNearestPosition(Translation2d to, boolean onRedAllianceSide) {
                Translation2d adjusted = (onRedAllianceSide) ? GeometryUtil.flipFieldPosition(to) : to;
                return Arrays.stream(validSpeakerShootingPositions).reduce(validSpeakerShootingPositions[0],
                        (min, element) -> (element.pose.getTranslation().getDistance(adjusted) < // If distance from given
                                min.pose.getTranslation().getDistance(adjusted)) ? // Is less than that of the previous smallest
                                element : min); // Return that new pose, otherwise pass along the smallest
            }
        }
    }

    public static class SwerveConstants {
        public static class ModuleConstants {
            public static final double wheelDiameterMeters = Units.inchesToMeters(4);
            public static final double driveMotorGearRatio = 1 / 6.75; // 6.75:1
            public static final double turningMotorGearRatio = 1 / (150.0 / 7); // 150/7:1
            /** The conversion factor for drive encoders from rotations to meters */
            public static final double driveEncoderRotToMeter = driveMotorGearRatio * Math.PI * wheelDiameterMeters;
            /** The conversion factor for turning encoders from rotations to radians */
            public static final double turningEncoderRotToRad = turningMotorGearRatio * 2 * Math.PI;
            /** The conversion factor for drive encoders from rotations per minute to meters per second */
            public static final double driveEncoderRPMToMeterPerSec = driveEncoderRotToMeter / 60;
            /** The conversion factor for turning encoders from rotations per minute to radians per second */
            public static final double turningEncoderRPMToRadPerSec = turningEncoderRotToRad / 60;

            /** The free speed of a drive wheel, in meters per second?
             * Retrieved from <a href=https://www.swervedrivespecialties.com/products/mk4i-swerve-module>SDS Specifications</a> */
            // Note: didn't change name of variable to meters per second, because I'm not really sure what it's in
            // This is a carry-over from REVLib, where it was implemented based on a ton of gear ratio conversions
            // https://github.com/REVrobotics/MAXSwerve-Java-Template/blob/169c7e33edac0a2fa3568d5442b6e73c79c33389/src/main/java/frc/robot/Constants.java#L78C5-L85C34
            // I did some rough math and figured the two numbers were arrived at the same way
            // ((motorFreeSpeed * wheelCircumference) / Gear reduction)
            public static final double driveWheelFreeSpeedRps = Units.feetToMeters(15.1);

            public static final double drivingP = 0.04;
            public static final double drivingI = 0;
            public static final double drivingD = 0;
            public static final double drivingFF = 1 / driveWheelFreeSpeedRps;
            public static final double drivingMinOutput = -1;
            public static final double drivingMaxOutput = 1;

            public static final double turningP = 1;
            public static final double turningI = 0;
            public static final double turningD = 0;
            public static final double turningFF = 0;
            public static final double turningMinOutput = -1;
            public static final double turningMaxOutput = 1;

            public static final IdleMode drivingMotorIdleMode = IdleMode.kBrake;
            public static final IdleMode turningMotorIdleMode = IdleMode.kBrake;

            public static final int drivingMotorCurrentLimit = 50; // amps
            public static final int turningMotorCurrentLimit = 20; // amps
        }
        public static class PortConstants {
            public static final int frontLeftDriveMotorPort = 6;
            public static final int backLeftDriveMotorPort = 3;
            public static final int frontRightDriveMotorPort = 9;
            public static final int backRightDriveMotorPort = 12;
            public static final int frontLeftTurningMotorPort = 4;
            public static final int backLeftTurningMotorPort = 1;
            public static final int frontRightTurningMotorPort = 7;
            public static final int backRightTurningMotorPort = 10;
            public static final int frontLeftDriveAbsoluteEncoderPort = 5;
            public static final int backLeftDriveAbsoluteEncoderPort = 2;
            public static final int frontRightDriveAbsoluteEncoderPort = 8;
            public static final int backRightDriveAbsoluteEncoderPort = 11;
        }
        public static class PhysicalConstants {
            // Chassis configuration
            // Distance between front and back wheels on robot
            public static final double trackWidth = Units.inchesToMeters(23.75);
            // Distance between centers of right and left wheels on robot
            public static final double wheelBase = Units.inchesToMeters(23.75);

            public static final double driveBaseRadius = Math.hypot(trackWidth / 2, wheelBase / 2);

            public static final boolean frontLeftTurningEncoderReversed = true;
            public static final boolean backLeftTurningEncoderReversed = true;
            public static final boolean frontRightTurningEncoderReversed = true;
            public static final boolean backRightTurningEncoderReversed = true;
            public static final boolean frontLeftDriveEncoderReversed = true;
            public static final boolean backLeftDriveEncoderReversed = true;
            public static final boolean frontRightDriveEncoderReversed = true;
            public static final boolean backRightDriveEncoderReversed = true;
            public static final boolean frontLeftDriveAbsoluteEncoderReversed = false;
            public static final boolean backLeftDriveAbsoluteEncoderReversed = false;
            public static final boolean frontRightDriveAbsoluteEncoderReversed = false;
            public static final boolean backRightDriveAbsoluteEncoderReversed = false;
            public static final double frontLeftDriveAbsoluteEncoderOffsetRad = 3.45145677;
            public static final double backLeftDriveAbsoluteEncoderOffsetRad = 2.61543724;
            public static final double frontRightDriveAbsoluteEncoderOffsetRad = 5.46250558;
            public static final double backRightDriveAbsoluteEncoderOffsetRad = 4.71699094;
            public static final double physicalMaxSpeedMetersPerSecond = 4.8;
            public static final double physicalMaxAngularSpeedRadiansPerSecond = 2 * 2 * Math.PI;
        }

        public static final SwerveDriveKinematics swerveDriveKinematics = new SwerveDriveKinematics(
                new Translation2d(PhysicalConstants.wheelBase / 2, PhysicalConstants.trackWidth / 2),
                new Translation2d(PhysicalConstants.wheelBase / 2, -PhysicalConstants.trackWidth / 2),
                new Translation2d(-PhysicalConstants.wheelBase / 2, PhysicalConstants.trackWidth / 2),
                new Translation2d(-PhysicalConstants.wheelBase / 2, -PhysicalConstants.trackWidth / 2));

        public static class TeleopConstants {
            public static final double teleDriveMaxSpeedMetersPerSecond = PhysicalConstants.physicalMaxSpeedMetersPerSecond;
            public static final double teleDriveMaxAngularSpeedRadiansPerSecond = //
                    PhysicalConstants.physicalMaxAngularSpeedRadiansPerSecond / 4;
            public static final double teleDriveMaxAccelerationUnitsPerSecond = 3;
            public static final double teleDriveMaxAngularAccelerationUnitsPerSecond = 3;
            public static final double teleopDrivingPTheta = 0.4;
            public static final double teleopDrivingITheta = 0;
            public static final double teleopDrivingDTheta = 0;
            public static final TrapezoidProfile.Constraints teleopDrivingThetaControllerConstraints =
                    new TrapezoidProfile.Constraints(
                            PhysicalConstants.physicalMaxAngularSpeedRadiansPerSecond / 5,
                            Math.PI);
        }

        public static class AutoConstants {
            public static final double maxSpeedMetersPerSecond = PhysicalConstants.physicalMaxSpeedMetersPerSecond / 4;
            public static final double maxAngularSpeedRadiansPerSecond =
                    PhysicalConstants.physicalMaxAngularSpeedRadiansPerSecond / 10;
            public static final double maxAccelerationMetersPerSecondSquared = 3;
            public static final double maxAngularAccelerationRadiansPerSecondSquared = Math.PI / 4;
            public static final double translationP = 5.0;
            public static final double translationI = 0.0;
            public static final double translationD = 0.0;
            public static final double rotationP = 5.0;
            public static final double rotationI = 0.0;
            public static final double rotationD = 0.0;

            public static final TrapezoidProfile.Constraints kThetaControllerConstraints = //
                    new TrapezoidProfile.Constraints(
                            maxAngularSpeedRadiansPerSecond,
                            maxAngularAccelerationRadiansPerSecondSquared);
        }
    }

    public static class ArmConstants {
        public static final int leftCanID = 16;
        public static final boolean leftInverted = true;

        public static final int rightCanID = 17;
        public static final boolean rightInverted = false;

        /** The port for the limit switch on the front (intake side) of the robot */
        public static final int frontLimitSwitchPort = 2;
        /** The port for the limit switch on the back (limelight side) of the robot */
        public static final int backLimitSwitchPort = 1;

        public static final int currentLimit = 40;

        public static final double frontLimit = 0.0;
        public static final double backLimit = 1.58;
        public static final double underStageLimit = 0.2;

        public static final double gearRatio = 1.0 / 256.0; // 256:1.
                                                                // Make sure both numbers are doubles (x.0, not just x)
                                                                // to avoid dividing by zero
        public static final double positionFactor =
                gearRatio
                        * 2.0
                        * Math.PI; // multiply ThroughBore value by this number and get arm position in radians
        public static final double velocityFactor = gearRatio * 2.0 * Math.PI / 60.0;
        public static final double armFreeSpeed = neoFreeSpeedRPM * velocityFactor;

        public static final ArmFeedforward armFeedforward =
                new ArmFeedforward(0.0, 0.47, 12.0 / armFreeSpeed, 0.0);
        public static final PIDGains armPositionGains = new PIDGains(0.6, 0.0, 0.0);
        public static final TrapezoidProfile.Constraints armMotionConstraint =
                new TrapezoidProfile.Constraints(2.0, 2.0);
    }

    public static class IntakeConstants {
        public static final int canID = 13;
        public static final boolean motorInverted = false;
        public static final int currentLimit = 20;
        public static final double intakeWheelDiameter = Units.inchesToMeters(2.0);
        public static final double motorPositionConversionFactorRotationToMeters = intakeWheelDiameter * Math.PI;

        public static final int breakbeamPort = 3;
        /** If the breakbeam reads true by default (when not blocked) */
        public static final boolean breakbeamTrueByDefault = true;

        public static final PIDGains positionGains = new PIDGains(1.0, 0.0, 0.0);
        public static final double positionTolerance = 0.5;

        public static final double intakePower = 0.7;

        /** The time after which a note past the breakbeam is clear of the launcher */
        public static double clearLauncherTime = 0.2;
    }

    public static class LauncherConstants {
        public static final int topCanId = 14;
        public static final int bottomCanId = 15;
        public static final boolean topMotorInverted = false;
        public static final boolean bottomMotorInverted = false;

        public static final int currentLimit = 80;
    }

    public static class ClimberConstants {
        public static final int leftCanId = 18;
        /** To let out (raise) the arm, the left motor will spin clockwise if false, counterclockwise if true.
         * It will run in the opposite direction when retracting (climbing). */
        public static final boolean leftInverted = true;

        public static final int rightCanId = 19;
        /** To let out (raise) the arm, the right motor will spin clockwise if false, counterclockwise if true.
         * It will run in the opposite direction when retracting (climbing). */
        public static final boolean rightInverted = false;

        /** The power to run the motors at to release (raise) the arms */
        public static final double releaseSpeed = 1;
        /** The power to run the motors at to climb (lower) the arms.
         * MAKE SURE THIS IS NEGATIVE!!! */
        public static final double climbSpeed = - // Don't you dare remove this negative sign
                                                0.5; // Change this number instead

        /** The time taken by the climber in seconds,
         * running at the given {@link frc.robot.Constants.ClimberConstants#releaseSpeed releaseSpeed},
         * to reach its highest point.
         */
        public static final double releaseToTopTimeSeconds = 0.0;
        /** The time taken by the climber in seconds,
         * running at the given {@link frc.robot.Constants.ClimberConstants#climbSpeed climbSpeed},
         * to retract from hits highest point to the bottom.
         */
        public static final double retractFullyDownTimeSeconds = 0.0;
    }

    public static final double neoFreeSpeedRPM = 5676;
}
