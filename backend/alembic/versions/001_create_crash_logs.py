"""create crash_logs table

Revision ID: 001_crash_logs
Revises:
Create Date: 2026-03-22
"""
from alembic import op
import sqlalchemy as sa

revision = "001_crash_logs"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "crash_logs",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("level", sa.String(20), server_default="error"),
        sa.Column("message", sa.Text(), nullable=False),
        sa.Column("stacktrace", sa.Text(), nullable=True),
        sa.Column("app_version", sa.String(20), nullable=True),
        sa.Column("device_info", sa.String(200), nullable=True),
        sa.Column("screen", sa.String(100), nullable=True),
        sa.Column("extra", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(), server_default=sa.func.now()),
    )
    op.create_index("ix_crash_logs_level", "crash_logs", ["level"])
    op.create_index("ix_crash_logs_created_at", "crash_logs", ["created_at"])


def downgrade() -> None:
    op.drop_index("ix_crash_logs_created_at", table_name="crash_logs")
    op.drop_index("ix_crash_logs_level", table_name="crash_logs")
    op.drop_table("crash_logs")
